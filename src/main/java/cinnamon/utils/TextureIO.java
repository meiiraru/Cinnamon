package cinnamon.utils;

import cinnamon.render.texture.Texture;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

import static cinnamon.Client.LOGGER;
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

            LOGGER.info("Saved screenshot as {}", path.getFileName());
        } catch (Exception e) {
            LOGGER.error("Failed to save screenshot!", e);
        }
    }

    public static void saveTexture(Texture texture, Path outputPath) {
        saveTexture(texture.getID(), outputPath, false, false);
    }

    public static void saveTexture(int texture, Path outputPath, boolean flipX, boolean flipY) {
        try {
            //bind texture
            glBindTexture(GL_TEXTURE_2D, texture);

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
                    img.setRGB(flipX ? width - x - 1 : x, flipY ? height - y - 1 : y, (a << 24) | (r << 16) | (g << 8) | b);
                }
            }

            //write file
            outputPath = IOUtils.parseNonDuplicatePath(outputPath);
            IOUtils.writeImage(outputPath, img);

            //unbind texture
            glBindTexture(GL_TEXTURE_2D, 0);

            LOGGER.info("Exported texture to {}", outputPath.getFileName());
        } catch (Exception e) {
            LOGGER.error("Failed to save texture!", e);
        }
    }

    public static ImageData load(Resource resource) throws Exception {
        return load(resource, false, 4);
    }

    public static ImageData load(Resource resource, boolean flip, int desiredChannels) throws Exception {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            STBImage.stbi_set_flip_vertically_on_load(flip);
            ByteBuffer imageBuffer = IOUtils.getResourceBuffer(resource);
            ByteBuffer buffer = STBImage.stbi_load_from_memory(imageBuffer, w, h, channels, desiredChannels);
            STBImage.stbi_set_flip_vertically_on_load(false);

            if (buffer == null)
                throw new Exception("Failed to load image \"" + resource + "\", " + STBImage.stbi_failure_reason());

            return new ImageData(w.get(), h.get(), buffer);
        }
    }

    public static class ImageData implements AutoCloseable {

        public final int width, height;
        public final ByteBuffer buffer;

        private ImageData(int width, int height, ByteBuffer buffer) {
            this.width = width;
            this.height = height;
            this.buffer = buffer;
        }

        @Override
        public void close() {
            STBImage.stbi_image_free(buffer);
        }
    }
}
