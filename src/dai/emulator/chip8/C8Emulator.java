package dai.emulator.chip8;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Random;

public class C8Emulator {

	int opCode; // unsigned 2 bytes
	int[] memory; // unsigned 1 bytes

	// 16 V registers, from V1 to VF
	int[] cpuRegisterV;

	int registerI; // 2 bytes
	byte Vx;
	byte Vy;
	int programCounter; // 2 bytes

	public int[] graphic;
	static boolean drawFlag;

	int delayTimer;
	int soundTimer;

	int[] stack;
	int stackPointer;

	int[] key;

	byte[] fontSet = { (byte) 0xF0, (byte) 0x90, (byte) 0x90, (byte) 0x90,
			(byte) 0xF0, // 0
			(byte) 0x20, (byte) 0x60, (byte) 0x20, (byte) 0x20, (byte) 0x70, // 1
			(byte) 0xF0, (byte) 0x10, (byte) 0xF0, (byte) 0x80, (byte) 0xF0, // 2
			(byte) 0xF0, (byte) 0x10, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, // 3
			(byte) 0x90, (byte) 0x90, (byte) 0xF0, (byte) 0x10, (byte) 0x10, // 4
			(byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, // 5
			(byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x90, (byte) 0xF0, // 6
			(byte) 0xF0, (byte) 0x10, (byte) 0x20, (byte) 0x40, (byte) 0x40, // 7
			(byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x90, (byte) 0xF0, // 8
			(byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, // 9
			(byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x90, (byte) 0x90, // A
			(byte) 0xE0, (byte) 0x90, (byte) 0xE0, (byte) 0x90, (byte) 0xE0, // B
			(byte) 0xF0, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0xF0, // C
			(byte) 0xE0, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xE0, // D
			(byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x80, (byte) 0xF0, // E
			(byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x80, (byte) 0x80 // F
	};

	public void init(String rom) {
		programCounter = 0x200;
		opCode = 0;
		registerI = 0;

		stack = new int[16];
		stackPointer = 0;

		graphic = new int[64 * 32];
		cpuRegisterV = new int[16];
		memory = new int[4096]; // 4K memory
		key = new int[16];

		// load fontSet, located in first 80 bytes
		for (int i = 0; i < 80; i++) {
			System.out.printf("i value is 0x%X\n", i);
			memory[i] = fontSet[i];
		}

		// load the program into memory
		byte[] buffer = new byte[4096];
		int totalRead = 0;
		try {
			File romFile = new File(rom);
			System.out.printf("file size is %d\n", romFile.length());

			FileInputStream romStream = new FileInputStream(romFile);
			totalRead = romStream.read(buffer);
			System.out.printf("read total of %d\n", totalRead);

			if (4096 - 512 > totalRead) {
				for (int i = 0; i < totalRead; i++) {					
					memory[i + 512] = buffer[i];
					if (memory[i+512] < 0){
						 memory[i+512] = memory[i+512] & 0x000000ff; 
					}
				}
			} else {
				System.out.println("Error: Rom too big for memory");
			}

			// reset timer;
			delayTimer = 0;
			romStream.close();

		} catch (Exception e) {
			System.out.println(e);
			return;
		}
	}

	private int Vx(int _opCode) {
		return cpuRegisterV[(_opCode & 0x0F00) >> 8];
	}

	private int Vy(int _opCode) {
		return cpuRegisterV[(_opCode & 0x00F0) >> 4];
	}

	public void emulateCycle() {
		// Fetch Opcode, fetch 2 bytes
		int op1= memory[programCounter] << 8;
		int op2 = memory[programCounter + 1];
		opCode = op1 | op2; 
		
		// Decode Opcode
		switch (opCode & 0xF000) {

		case 0x0000:
			switch (opCode & 0x00FF) {
			case 0x00E0: // 1
				// clear screen
				for(int i = 0; i < 2048; i++){
					graphic[i] = 0x0;
					drawFlag = true;
					programCounter += 2;
				}
				break;
			case 0x00EE: // 2
				// return from subroutine
				--stackPointer;
				programCounter = stack[stackPointer];
				programCounter += 2;
				break;
			default:
				System.out.printf("Unknown opcode: 0x%X\n", opCode);
			}
			break;
		// 0nnn - SYS addr

		// 1nnn - JP addr
		case 0x1000:
			programCounter = opCode & 0x0FFF; // 3
			break;

		// 2nnn - CALL addr
		case 0x2000: // 4
			stack[stackPointer] = programCounter;
			stackPointer++;
			programCounter = opCode & 0x0FFF;
			break;

		// 3xkk - SE Vx, byte
		case 0x3000: // 5
			if (cpuRegisterV[(opCode & 0x0F00) >> 8] == (opCode & 0x00FF))
				programCounter += 4;
			else
				programCounter += 2;
			break;
		// 4xkk - SNE Vx, byte
		case 0x4000: // 6
			if (cpuRegisterV[(opCode & 0x0F00) >> 8] != (opCode & 0x00FF))
				programCounter += 4;
			else
				programCounter += 2;
			break;
		// 5xy0 - SE Vx, Vy
		case 0x5000: // 7
			if (cpuRegisterV[(opCode & 0x0F00) >> 8] == cpuRegisterV[(opCode & 0x00F0) >> 4])
				programCounter += 4;
			else
				programCounter += 2;
			break;
		// 6xkk - LD Vx, byte
		case 0x6000:// 8
			cpuRegisterV[(opCode & 0x0F00) >> 8] = (byte) (opCode & 0x00FF);
			programCounter += 2;
			break;
		// 7xkk - ADD Vx, byte
		case 0x7000:// 9
			cpuRegisterV[(opCode & 0x0F00) >> 8] += (byte) (opCode & 0x00FF);
			programCounter += 2;
			break;
		case 0x8000:
			switch (opCode & 0x000F) {
			// 8xy0 - LD Vx, Vy
			case 0x0000: // 10
				cpuRegisterV[(opCode & 0x0F00) >> 8] = cpuRegisterV[(opCode & 0x00F0) >> 4];
				programCounter += 2;
				break;
			// 8xy1 - OR Vx, Vy
			case 0x0001: // 11
				cpuRegisterV[(opCode & 0x0F00) >> 8] |= cpuRegisterV[(opCode & 0x00F0) >> 4];
				programCounter += 2;
				break;
			// 8xy2 - AND Vx, Vy
			case 0x0002: // 12
				cpuRegisterV[(opCode & 0x0F00) >> 8] &= cpuRegisterV[(opCode & 0x00F0) >> 4];
				programCounter += 2;
				break;
			// 8xy3 - XOR Vx, Vy
			case 0x0003:// 13
				cpuRegisterV[(opCode & 0x0F00) >> 8] ^= cpuRegisterV[(opCode & 0x00F0) >> 4];
				programCounter += 2;
				break;
			// 8xy4 - ADD Vx, Vy
			case 0x0004: // 14
				if (cpuRegisterV[(opCode & 0x00F0) >> 4] > (0xFF - cpuRegisterV[(opCode & 0x0F00) >> 8])) {
					cpuRegisterV[0xF] = 1;
				} else {
					cpuRegisterV[0xF] = 0;
				}
				cpuRegisterV[(opCode & 0x0F00) >> 8] |= cpuRegisterV[(opCode & 0x00F0) >> 4];
				programCounter += 2;
				break;
			// 8xy5 - SUB Vx, Vy
			case 0x0005: // 15
				if (cpuRegisterV[(opCode & 0x00F0) >> 4] > (0xFF - cpuRegisterV[(opCode & 0x0F00) >> 8])) {
					cpuRegisterV[0xF] = 0;
				} else {
					cpuRegisterV[0xF] = 1;
				}
				cpuRegisterV[(opCode & 0x0F00) >> 8] -= cpuRegisterV[(opCode & 0x00F0) >> 4];
				programCounter += 2;
				break;
			// 8xy6 - SHR Vx {, Vy}
			case 0x0006: // 16
				cpuRegisterV[0xF] = (byte) (cpuRegisterV[(opCode & 0x0F00) >> 8] & 0x1);
				cpuRegisterV[(opCode & 0x0F00) >> 8] >>= 1;
				programCounter += 2;
				break;
			// 8xy7 - SUBN Vx, Vy
			case 0x0007: // 17
				if (cpuRegisterV[(opCode & 0x00F0) >> 4] > (0xFF - cpuRegisterV[(opCode & 0x0F00) >> 8])) {
					cpuRegisterV[0xF] = 1;
				} else {
					cpuRegisterV[0xF] = 0;
				}
				cpuRegisterV[(opCode & 0x0F00) >> 8] = (byte) (cpuRegisterV[(opCode & 0x00F0) >> 4] - cpuRegisterV[(opCode & 0x0F00) >> 8]);
				programCounter += 2;
				break;
			// 8xyE - SHL Vx {, Vy}
			case 0x000E:// 18
				cpuRegisterV[0xF] = (byte) (cpuRegisterV[(opCode & 0x0F00) >> 8] >> 7);
				cpuRegisterV[(opCode & 0x0F00) >> 8] <<= 1;
				programCounter += 2;
				break;
			default:
				System.out.printf("Unknown opcode: 0x%X\n", opCode);
			}
			break;

		// 9xy0 - SNE cpuRegisterV[(opCode & 0x0F00) >> 8]Vx, Vy
		case 0x9000:// 19
			if (cpuRegisterV[(opCode & 0x0F00) >> 8] != cpuRegisterV[(opCode & 0x00F0) >> 4]) {
				programCounter += 4;
			} else {
				programCounter += 2;
			}
			break;
		// ANNN: sets I to the address NNN
		case 0xA000:// 20
			registerI = (char) (opCode & 0x0FFF);
			programCounter += 2;
			break;
		// Bnnn - JP V0, addr
		case 0xB000:// 21
			programCounter += (opCode & 0x0FFF) + cpuRegisterV[0];
			break;
		// Cxkk - RND Vx, byte
		case 0xC000:// 22
			Random generator = new Random();
			cpuRegisterV[(opCode & 0x0F00) >> 8] = (byte) (generator
					.nextInt(256) & (opCode & 0x00FF));
			programCounter += 2;
			break;
		// Dxyn - DRW Vx, Vy, nibble
		/*
		 * Dxyn - DRW Vx, Vy, nibble Display n-byte sprite starting at memory
		 * location I at (Vx, Vy), set VF = collision. The interpreter reads n
		 * bytes from memory, starting at the address stored in I. These bytes
		 * are then displayed as sprites on screen at coordinates (Vx, Vy).
		 * Sprites are XORed onto the existing screen. If this causes any pixels
		 * to be erased, VF is set to 1, otherwise it is set to 0. If the sprite
		 * is positioned so part of it is outside the coordinates of the
		 * display, it wraps around to the opposite side of the screen. See
		 * instruction 8xy3 for more information on XOR, and section 2.4,
		 * Display, for more information on the Chip-8 screen and sprites.
		 */
		case 0xD000: // 23

			int x = Vx(opCode);
			int y = Vy(opCode);
			int height = opCode & 0x000F;
			int pixel = 0;
			cpuRegisterV[0xF] = 0;

			for (int yline = 0; yline < height; yline++) {
				pixel = memory[registerI + yline];
				for (int xline = 0; xline < 8; xline++) {
					if ((pixel & (0x80 >> xline)) != 0) {
						if (graphic[(x + xline + ((y + yline) * 64))] == 1) {
							cpuRegisterV[0xF] = 1;
						}
						graphic[x + xline + ((y + yline) * 64)] ^= 1;
					}
				}
			}

			drawFlag = true;
			programCounter += 2;
			break;

		case 0xE000:
			switch (opCode & 0xF0FF) {
			// Ex9E - SKP Vx
			case 0xE09E: // 24
				if (key[Vx(opCode)] != 0)
					programCounter += 4;
				else
					programCounter += 2;
				break;
			// ExA1 - SKNP Vx
			case 0xE0A1: // 25
				if (key[Vx(opCode)] == 0)
					programCounter += 4;
				else
					programCounter += 2;
				break;
			default:
				System.out.printf("Unknown opcode: 0x%X\n", opCode);
			}
			break;

		case 0xF000:
			switch (opCode & 0xF0FF) {
			// Fx07 - LD Vx, DT
			case 0xF007:// 26
				cpuRegisterV[(opCode & 0x0F00) >> 8] = delayTimer;
				programCounter += 2;
				break;
			// Fx0A - LD Vx, K, wait for a key press, then load to Vx
			case 0xF00A:// 34
				boolean keyPress = false;
				for (byte i = 0; i < 16; i++) {
					if (key[i] == 1) {
						cpuRegisterV[(opCode & 0x0F00) >> 8] = i;
						keyPress = true;
					}
				}
				if (!keyPress) {
					return;
				}

				programCounter += 2;
				break;
			// Fx15 - LD DT, Vx
			case 0xF015:// 27
				delayTimer = cpuRegisterV[(opCode & 0x0F00) >> 8];
				programCounter += 2;
				break;
			// Fx18 - LD ST, Vxprintf
			case 0xF018:// 28
				soundTimer = Vx(opCode);
				break;
			// Fx1E - ADD I, Vx
			case 0xF01E:// 29
				if (registerI + Vx(opCode) > 0xfff) {
					cpuRegisterV[0xf] = 1;
				} else {
					cpuRegisterV[0xf] = 0;
				}
				registerI += Vx(opCode);
				programCounter += 2;
				break;
			// Fx29 - LD F, Vx
			case 0xF029:// 30
				registerI = Vx(opCode) * 0x5;
				programCounter += 2;
				break;
			// Fx33 - LD B, Vx
			case 0xF033:// 31
				memory[registerI] = cpuRegisterV[(opCode & 0x0F00) >> 8] / 100;
				memory[registerI + 1] = (cpuRegisterV[(opCode & 0x0F00) >> 8] / 10) % 10;
				memory[registerI + 2] = (cpuRegisterV[(opCode & 0x0F00) >> 8] % 100) % 10;
				programCounter += 2;
				break;
			// Fx55 - LD [I], Vx
			case 0xF055:// 32
				for (int i = 0; i < ((opCode & 0x0f00) >> 8); i++) {
					memory[registerI + i] = cpuRegisterV[i];
				}
				registerI += ((opCode & 0x0f00) >> 8) + 1;
				programCounter += 2;
				break;
			// Fx65 - LD Vx, [I]
			case 0xF065:// 33
				for (int i = 0; i < ((opCode & 0x0f00) >> 8); i++) {
					cpuRegisterV[i] = memory[registerI + i];
				}
				registerI += ((opCode & 0x0f00) >> 8) + 1;
				programCounter += 2;
				break;
			default:
				System.out.printf("Unknown opcode: 0x%X\n", opCode);				
			}
			break;
		default:
			System.out.printf("Unknown opcode: 0x%X\n", opCode);
		}

		// Execute Opcode

		// Update timers
		if (delayTimer > 0) {
			--delayTimer;
		}
		if (soundTimer > 0) {
			if (soundTimer == 1) {
				System.out.println("BEEP!\n");
			}

			--soundTimer;
		}
	}

	private void debugRender() {
		// Draw
		for (int y = 0; y < 32; ++y) {
			for (int x = 0; x < 64; ++x) {
				if (graphic[(y * 64) + x] == 0)
					System.out.print("O");
				else
					System.out.println("1");
			}
			System.out.println("\n");
		}		
	}

	public static void main(String[] args) {
		System.out.println("helllo");
		System.out.println(args);
		C8Emulator chip8 = new C8Emulator();
		// load file to mem
		chip8.init(args[0]);

		while (true) {
			chip8.emulateCycle();
			// draw
			if (drawFlag) {
				chip8.debugRender();
				drawFlag = false;
			}
		}

	}

}

