package dai.emulator.chip8;

import static javax.media.opengl.GL.GL_COLOR_BUFFER_BIT;
import static javax.media.opengl.GL.GL_DEPTH_TEST;
import static javax.media.opengl.GL.GL_LEQUAL;
import static javax.media.opengl.GL.GL_NICEST;
import static javax.media.opengl.GL2ES1.GL_PERSPECTIVE_CORRECTION_HINT;
import static javax.media.opengl.GL2GL3.GL_QUADS;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_SMOOTH;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLJPanel;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.gl2.GLUT;

@SuppressWarnings("serial")
public class C8Gui extends GLJPanel implements GLEventListener, KeyListener {

	private static String TITLE = "Chip8 d-emulator";
	private static int PIXEL_WIDTH = 15;
	private static int PIXEL_UNIT = 600;
	private static int CANVAS_WIDTH = PIXEL_WIDTH * 64;
	private static int CANVAS_HEIGHT = PIXEL_WIDTH * 32;
	private static int FPS = 200;
	private C8Emulator chip8;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				GLJPanel canvas = new C8Gui();
				canvas.setPreferredSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));

				final FPSAnimator animator = new FPSAnimator(canvas, FPS, true);
				final JFrame frame = new JFrame();
				frame.getContentPane().add(canvas);
				frame.addWindowListener(new WindowAdapter() {

					public void windowClosing(WindowEvent e) {
						new Thread() {
							public void run() {
								if (animator.isAnimating()) {
									animator.stop();
								}
							}
						}.start();
					}
				});

				frame.setTitle(TITLE);
				frame.pack();
				frame.setVisible(true);
				animator.start();
			}
		});
	}

	public C8Gui() {
		this.addGLEventListener(this);
		this.addKeyListener(this);
		this.setFocusable(true);
		this.requestFocus();
		chip8 = new C8Emulator();
		chip8.init("/home/dai-network-lab/Dropbox/dev/emulation/demulator/resources/tetris.c8");
	}

	private GLU glu;
	private GLUT glut;

	@Override
	public void init(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		glu = new GLU();
		glut = new GLUT();
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glClearDepthf(1.0f);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);
		gl.glShadeModel(GL_SMOOTH);

	}

	@Override
	public void display(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		// gl.glClearColor(1, 1, 1, 0);
		// gl.glClear(GL_COLOR_BUFFER_BIT);

		gl.glMatrixMode(GL_MODELVIEW);
		gl.glLoadIdentity();

		// gl.glColor3f(0, 0, 1);
		// gl.glRectd(20.0, 0.0, 10.0, 30.0);

		chip8.emulateCycle();
		if (chip8.drawFlag) {
			updateQuads(gl);
			chip8.drawFlag = false;
		}

	}

	public void drawPixel(GL2 gl, int x, int y) {
		int modifier = 10;

		// gl.glRectd(x, y, x*modifier, y*modifier);

		gl.glBegin(GL_QUADS);
		gl.glVertex3f((x * modifier) + 0.0f, (y * modifier) + 0.0f, 0.0f);
		gl.glVertex3f((x * modifier) + 0.0f, (y * modifier) + modifier, 0.0f);
		gl.glVertex3f((x * modifier) + modifier, (y * modifier) + modifier, 0.0f);
		gl.glVertex3f((x * modifier) + modifier, (y * modifier) + 0.0f, 0.0f);
		gl.glEnd();
	}

	public void updateQuads(GL2 gl) {
		// Draw
		for (int y = 0; y < 32; ++y)
			for (int x = 0; x < 64; ++x) {
				if (chip8.graphic[(y * 64) + x] == 0) {

					gl.glColor3f(1.0f, 1.0f, 1.0f);
				} else {
					gl.glColor3f(0.0f, 0.0f, 0.0f);
				}

				drawPixel(gl, x, y);
			}
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {

	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glClearColor(0, 0, 0, 0);
		gl.glClear(GL_COLOR_BUFFER_BIT);

		gl.glMatrixMode(GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluOrtho2D(0.0, width, height, 0);
		gl.glViewport(0, 0, width, height);

	}

	
	@Override
	public void keyTyped(java.awt.event.KeyEvent e) {		
	}

	@Override
	public void keyPressed(java.awt.event.KeyEvent e) {
		int keycode = e.getKeyCode();
		switch (keycode) {
		case java.awt.event.KeyEvent.VK_ESCAPE:
			new Thread() {
				@Override
				public void run() {
					GLAnimatorControl animator = getAnimator();
					if (animator.isStarted()) {
						animator.stop();
					}
					System.exit(0);
				}
			}.start();
			break;
			
		case KeyEvent.VK_1:
			chip8.key[0x1] = 1;
			break;
		case KeyEvent.VK_2:
			chip8.key[0x2] = 1;
			break;
		case KeyEvent.VK_3:
			chip8.key[0x3] = 1;
			break;
		case KeyEvent.VK_4:
			chip8.key[0xC] = 1;
			break;
		case KeyEvent.VK_Q:
			chip8.key[0x4] = 1;
			break;
		case KeyEvent.VK_W:
			chip8.key[0x5] = 1;
			break;
		case KeyEvent.VK_E:
			chip8.key[0x6] = 1;
			break;
		case KeyEvent.VK_R:
			chip8.key[0xD] = 1;
			break;
		case KeyEvent.VK_A:
			chip8.key[0x7] = 1;
			break;
		case KeyEvent.VK_S:
			chip8.key[0x8] = 1;
			break;
		case KeyEvent.VK_D:
			chip8.key[0x9] = 1;
			break;
		case KeyEvent.VK_F:
			chip8.key[0xE] = 1;
			break;
		case KeyEvent.VK_Z:
			chip8.key[0xA] = 1;
			break;
		case KeyEvent.VK_X:
			chip8.key[0x0] = 1;
			break;
		case KeyEvent.VK_C:
			chip8.key[0xB] = 1;
			break;
		case KeyEvent.VK_V:
			chip8.key[0xF] = 1;
			break;
		
		default:
			break;
		}

	}

	@Override
	public void keyReleased(java.awt.event.KeyEvent e) {
		int keycode = e.getKeyCode();
		switch (keycode) {
		case KeyEvent.VK_1:
			chip8.key[0x1] = 0;
			break;
		case KeyEvent.VK_2:
			chip8.key[0x2] = 0;
			break;
		case KeyEvent.VK_3:
			chip8.key[0x3] = 0;
			break;
		case KeyEvent.VK_4:
			chip8.key[0xC] = 0;
			break;
		case KeyEvent.VK_Q:
			chip8.key[0x4] = 0;
			break;
		case KeyEvent.VK_W:
			chip8.key[0x5] = 0;
			break;
		case KeyEvent.VK_E:
			chip8.key[0x6] = 0;
			break;
		case KeyEvent.VK_R:
			chip8.key[0xD] = 0;
			break;
		case KeyEvent.VK_A:
			chip8.key[0x7] = 0;
			break;
		case KeyEvent.VK_S:
			chip8.key[0x8] = 0;
			break;
		case KeyEvent.VK_D:
			chip8.key[0x9] = 0;
			break;
		case KeyEvent.VK_F:
			chip8.key[0xE] = 0;
			break;
		case KeyEvent.VK_Z:
			chip8.key[0xA] = 0;
			break;
		case KeyEvent.VK_X:
			chip8.key[0x0] = 0;
			break;
		case KeyEvent.VK_C:
			chip8.key[0xB] = 0;
			break;
		case KeyEvent.VK_V:
			chip8.key[0xF] = 0;
			break;
		default:
			break;
		}
	}
}
