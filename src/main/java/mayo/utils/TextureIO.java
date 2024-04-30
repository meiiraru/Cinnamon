package mayo.utils;

import mayo.render.Texture;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.lwjgl.opengl.GL11.*;

public class TextureIO {

    public static void screenshot(int width, int height) {
        try {
            //allocate buffer
            ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);

            //copy current frame data
            glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

            //write image to the buffer
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int i = (x + (height - y - 1) * width) * 4;
                    int r = buffer.get(i) & 0xFF;
                    int g = buffer.get(i + 1) & 0xFF;
                    int b = buffer.get(i + 2) & 0xFF;
                    int a = buffer.get(i + 3) & 0xFF;
                    img.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                }
            }

            //save as screenshot_yyyy-MM-dd_HH-mm-ss.png
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String fileName = "screenshot_" + sdf.format(new Date()) + ".png";
            Path path = IOUtils.parseNonDuplicatePath(IOUtils.ROOT_FOLDER.resolve("screenshots/" + fileName));

            //write file
            IOUtils.writeImage(path, img);

            System.out.println("Saved screenshot as " + path.getFileName());
        } catch (Exception e) {
            System.out.println("Failed to save screenshot!");
            e.printStackTrace();
        }
    }

    public static void saveTexture(Texture texture, Path outputPath) {
        try {
            //bind texture
            glBindTexture(GL_TEXTURE_2D, texture.getID());

            //grab width and height
            int width = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH);
            int height = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT);

            //allocate buffer
            ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);

            //copy texture data
            glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

            //write image to the buffer
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int i = (x + y * width) * 4;
                    int r = buffer.get(i) & 0xFF;
                    int g = buffer.get(i + 1) & 0xFF;
                    int b = buffer.get(i + 2) & 0xFF;
                    int a = buffer.get(i + 3) & 0xFF;
                    img.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                }
            }

            //write file
            IOUtils.writeImage(outputPath, img);

            //unbind texture
            glBindTexture(GL_TEXTURE_2D, 0);

            System.out.println("Exported texture to " + outputPath.getFileName());
        } catch (Exception e) {
            System.out.println("Failed to save texture!");
            e.printStackTrace();
        }
    }
}
