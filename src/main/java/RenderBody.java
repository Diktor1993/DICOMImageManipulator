import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLU;
import ij.plugin.DICOM;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;

public class RenderBody extends GLCanvas implements GLEventListener {

    private static GL2 gl;
    private static GLProfile profile;
    private static int width;
    private static int height;
    private static int bitsAllocation;
    private static DICOM image;
    private static GLAutoDrawable glAutoDrawable;
    private static GLCanvas canvas;
    private static byte[] buffer;
    private static byte[] RGBABuffer;
    private static byte[] mask;
    private static double deltaX = 0;
    private static double angle = 0;
    private static double countDeltaX;
    private static double countAngle;
    private static TextField oXTF;
    private static TextField angleTF;
    private static double[][] translationArr = new double[4][4];
    private static double[][] rotationArr = new double[4][4];
    private static double[][] current2DimArr = new double[4][4];
    private static double[] current1DimArr = new double[16];



    RenderBody(){}

    public static void main(String[] args) {
        RenderBody renderBody = new RenderBody();
        renderBody.createGUI();
    }

    private void createGUI(){
        RenderBody renderBody = new RenderBody();
        try {
            textureIdentity();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities capabilities = new GLCapabilities(profile);
        canvas = new GLCanvas(capabilities);
        canvas.addGLEventListener(renderBody);
        canvas.setSize(height, width);
        canvas.addKeyListener(new Keyboard());
        canvas.setFocusable(true);
        final JFrame frame = new JFrame("Image");

        final JPanel paramsPanel = new JPanel();
        paramsPanel.setBackground(Color.BLACK);
        paramsPanel.setLayout(null);
        oXTF = new TextField();
        oXTF.setVisible(true);
        oXTF.setBackground(Color.white);
        oXTF.setForeground(Color.black);
        oXTF.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        oXTF.setBounds(0, 255, 119, 25);
        oXTF.setText("   oX range");
        angleTF = new TextField();
        angleTF.setVisible(true);
        angleTF.setBackground(Color.white);
        angleTF.setForeground(Color.black);
        angleTF.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        angleTF.setBounds(130, 255, 119, 25);
        angleTF.setText("   angle");

        paramsPanel.setVisible(true);
        paramsPanel.setSize(width, 44);
        paramsPanel.add(oXTF);
        paramsPanel.add(angleTF);

        frame.getContentPane().setBackground(new Color(0,0,0));
        frame.setSize(height, width + 54);
        frame.getContentPane().add(canvas);
        frame.getContentPane().add(paramsPanel);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    }

    public void display(GLAutoDrawable gLDrawable) {

        glAutoDrawable = gLDrawable;
        GLU glu = new GLU();
        gl = gLDrawable.getGL().getGL2();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT );
        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        gl.glClearColor(0, 0, 0, 1);
        gl.glLoadIdentity();
        glu.gluOrtho2D(-width, width, -height, height);
        try {
            handleTheTexture(gl);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if(angle != 0 || deltaX != 0) {
            gl.glMultMatrixd(current1DimArr, 0);
        }
        texturePositions(gl);
    }

    private void texturePositions(final GL2 gl) {

        gl.glBegin(GL2.GL_QUADS);
        gl.glTexCoord2d(0, 1);
        gl.glVertex3d(-height/2, -width/2, 0);
        gl.glTexCoord2d(0, 0);
        gl.glVertex3d(-height/2, width/2, 0);
        gl.glTexCoord2d(1, 0);
        gl.glVertex3d(height/2, width/2, 0);
        gl.glTexCoord2d(1, 1);
        gl.glVertex3d(height/2, -width/2, 0);
        gl.glEnd();
        gl.glFlush();
        gl.glDisable(GL2.GL_TEXTURE_2D);
    }

    private void textureIdentity() throws FileNotFoundException {
        image = new DICOM(new FileInputStream(new File("src\\main\\resources\\Image.dcm")));
        image.run("src\\main\\resources\\Image.dcm");
        bitsAllocation = Integer.parseInt((image.getStringProperty("0028,0100")).trim());
        width = Integer.parseInt((image.getStringProperty("0028,0010")).trim());
        height = Integer.parseInt((image.getStringProperty("0028,0011")).trim());
        buffer = ((DataBufferByte) image.getBufferedImage().getData().getDataBuffer()).getData();
    }

    private void handleTheTexture(GL2 gl) throws FileNotFoundException {

        if (bitsAllocation == 8) {
            ByteBuffer buffer = ByteBuffer.allocate(height * width);
            ByteBuffer wrapArray = buffer.wrap(this.buffer, 0, height * width);
            final int[] textureID = new int[1];
            gl.glGenTextures(1, textureID, 0);
            gl.glBindTexture(GL2.GL_TEXTURE_2D, textureID[0]);
            gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
            gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
            gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_LUMINANCE, height, width, 0, GL2.GL_LUMINANCE,
                    GL2.GL_UNSIGNED_BYTE, wrapArray);
            gl.glEnable(GL2.GL_TEXTURE_2D);
        } else {
            ByteBuffer buffer = ByteBuffer.allocate(height * width * 4);
            ByteBuffer wrapArray = buffer.wrap(RGBABuffer, 0, height * width * 4);
            final int[] textureID = new int[1];
            gl.glGenTextures(1, textureID, 0);
            gl.glBindTexture(GL2.GL_TEXTURE_2D, textureID[0]);
            gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
            gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
            gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA, height, width, 0, GL2.GL_RGBA,
                    GL2.GL_UNSIGNED_BYTE, wrapArray);
            gl.glEnable(GL2.GL_TEXTURE_2D);
        }
    }


    public void transRotate(){

        double radAngle = Double.parseDouble(angleTF.getText())*(Math.PI/180);

        angle = radAngle;
        deltaX = Double.parseDouble(oXTF.getText());

        countAngle = angle;
        countDeltaX = deltaX;

        translationArr[0][0] = translationArr[1][1] = translationArr[2][2] = translationArr[3][3] = 1;
        rotationArr[2][2] = rotationArr[3][3] = 1;

        translationArr[0][3] = deltaX;

        rotationArr[0][0] = Math.cos(angle);
        rotationArr[1][0] = - Math.sin(angle);
        rotationArr[0][1] = Math.sin(angle);
        rotationArr[1][1] = Math.cos(angle);

        double[][] tempArr = new double[4][4];

        for(int i = 0; i < translationArr.length; i++) {
            for (int j = 0; j < rotationArr[i].length; j++) {
                for (int k = 0; k < translationArr.length; k++) {
                    tempArr[i][j] += translationArr[i][k] * rotationArr[k][j];
                }
            }
        }

        current2DimArr = tempArr;

        for(int i = 0; i < current2DimArr.length; i++){
            for(int j = 0; j < current2DimArr[0].length; j++) {
                current1DimArr[i * current2DimArr[0].length + j] = current2DimArr[j][i];
            }
        }

    }

    public void invertTransRotat() {

        if(deltaX > 0) {
            deltaX = countDeltaX - deltaX;
        }else{
            deltaX = 0;
        }
        if(angle > 0){
            angle = countAngle - angle;
        }else{
            angle = 0;
        }

        translationArr[0][3] = deltaX;
        rotationArr[0][0] = Math.cos(angle);
        rotationArr[1][0] = - Math.sin(angle);
        rotationArr[0][1] = Math.sin(angle);
        rotationArr[1][1] = Math.cos(angle);

        double[][] tempArr = new double[4][4];

        for(int i = 0; i < translationArr.length; i++) {
            for (int j = 0; j < rotationArr[i].length; j++) {
                for (int k = 0; k < translationArr.length; k++) {
                    tempArr[i][j] += translationArr[i][k] * rotationArr[k][j];
                }
            }
        }

        current2DimArr = tempArr;

        for(int i = 0; i < current2DimArr.length; i++){
            for(int j = 0; j < current2DimArr[0].length; j++) {
                current1DimArr[i * current2DimArr[0].length + j] = current2DimArr[j][i];
            }
        }

        oXTF.setText("0");
        angleTF.setText("0");

    }


    public void RGBAConvert(){
        buffer = ((DataBufferByte) image.getBufferedImage().getData().getDataBuffer()).getData();
        RGBABuffer = new byte[height * width * 4];
        for (int i = 4; i < RGBABuffer.length; i += 4) {
            RGBABuffer[i - 4] = buffer[(i/4) - 1];
            RGBABuffer[i - 3] = 0;
            RGBABuffer[i - 2] = 0;
            RGBABuffer[i - 1] = 0;
        }
    }

    public void RGBAGradConvert(){
        buffer = ((DataBufferByte) image.getBufferedImage().getData().getDataBuffer()).getData();
        RGBABuffer = new byte[height * width * 4];
        for (int i = 4; i < RGBABuffer.length; i+=4) {
            if (buffer[(i/4) - 1] > 127) {
                RGBABuffer[i - 4] = (byte) ((buffer[(i / 4) - 1])*2);
            }else {
                RGBABuffer[i - 4] = (byte) ((255 - (buffer[(i / 4) - 1]))*2);
            }
            RGBABuffer[i - 3] = 0;
            RGBABuffer[i - 2] = 0;
            RGBABuffer[i - 1] = 0;
        }
    }

    public byte[] createMask(){
        buffer = ((DataBufferByte) image.getBufferedImage().getData().getDataBuffer()).getData();
        for (int j = 0; j < buffer.length; j++) {
            if (((j)% width) < (width)/2){
                buffer[j] = 0;
            }
        }
        return buffer;
    }

    public void init(GLAutoDrawable gLDrawable) {}

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {}

    public void reshape(GLAutoDrawable gLDrawable, int x, int y, int width, int height) {
        glAutoDrawable = gLDrawable;
        gl = gLDrawable.getGL().getGL2();
        GLU glu = new GLU();
        glu.gluOrtho2D(-height, height, -width, width);
        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
    }

    private class Keyboard implements KeyListener{
        @Override
        public void keyTyped(KeyEvent e) {

        }

        @Override
        public void keyPressed(KeyEvent e) {
            if(e.getKeyCode()== KeyEvent.VK_1){
                bitsAllocation = 8;
                buffer = ((DataBufferByte) image.getBufferedImage().getData().getDataBuffer()).getData();
                canvas.repaint();
            }else if(e.getKeyCode()== KeyEvent.VK_2){
                bitsAllocation = 8;
                mask = createMask();
                buffer = ((DataBufferByte) image.getBufferedImage().getData().getDataBuffer()).getData();
                for (int i = 0; i < buffer.length; i++) {
                    buffer[i] = ( mask[i]);
                }
                canvas.repaint();
            }else if(e.getKeyCode()== KeyEvent.VK_3 ){
                bitsAllocation = 16;
                RGBAConvert();
                canvas.repaint();
            }else if (e.getKeyCode()== KeyEvent.VK_4) {
                bitsAllocation = 16;
                RGBAGradConvert();
                canvas.repaint();
            }else if(e.getKeyCode() == KeyEvent.VK_5){
                bitsAllocation = 8;
                invertTransRotat();
                canvas.display();
            }else if(e.getKeyCode() == KeyEvent.VK_6){
                bitsAllocation = 8;
                transRotate();
                canvas.display();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {

        }
    }
}
