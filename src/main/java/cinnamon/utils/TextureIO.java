package cinnamon.utils;

import cinnamon.render.texture.Texture;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.AITexture;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

import static cinnamon.Client.LOGGER;
import static org.lwjgl.opengl.GL11.*;

/**
 * Utility class for I/O operations related to textures
 */
public final class TextureIO {

    private TextureIO() {}

    /**
     * Returns the path to the animation file corresponding to the given resource
     * @param resource The {@link Resource} for which to find the animation file
     * @return The {@link Resource} representing the animation file, or {@code null} if no animation file exists
     */
    public static Resource getAnimationPath(Resource resource) {
        Resource anim = resource.resolveSibling(resource.getFileName() + ".json");
        return IOUtils.hasResource(anim) ? anim : null;
    }

    /**
     * Takes a screenshot of the current OpenGL frame and saves it as a PNG file in the "screenshots" folder
     * @param width The width of the screenshot in pixels
     * @param height The height of the screenshot in pixels
     * @return The {@link Path} to the saved screenshot file, or {@code null} if the screenshot could not be saved
     */
    public static Path screenshot(int width, int height) {
        try {
            //allocate buffer
            ByteBuffer buffer = MemoryUtil.memAlloc(width * height * 4);

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

            MemoryUtil.memFree(buffer);
            LOGGER.info("Saved screenshot as \"%s\"", path.getFileName());

            return path;
        } catch (Exception e) {
            LOGGER.error("Failed to save screenshot!", e);
        }

        return null;
    }

    /**
     * Saves the given {@link Texture} to the specified output path as a PNG file
     * @param texture The {@link Texture} to save
     * @param outputPath The {@link Path} where the texture should be saved
     * @return {@code true} if the texture was saved successfully, {@code false} otherwise
     * @see #saveTexture(int, Path, boolean, boolean)
     */
    public static boolean saveTexture(Texture texture, Path outputPath) {
        return saveTexture(texture.getID(), outputPath, false, false);
    }

    /**
     * Saves the given OpenGL texture to the specified output path as a PNG file
     * @param texture The OpenGL texture ID to save
     * @param outputPath The {@link Path} where the texture should be saved
     * @param flipX Whether to flip the texture horizontally
     * @param flipY Whether to flip the texture vertically
     * @return {@code true} if the texture was saved successfully, {@code false} otherwise
     * @see #saveTexture(Texture, Path)
     */
    public static boolean saveTexture(int texture, Path outputPath, boolean flipX, boolean flipY) {
        try {
            //bind texture
            glBindTexture(GL_TEXTURE_2D, texture);

            //grab width and height
            int width = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH);
            int height = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT);

            //allocate buffer
            ByteBuffer buffer = MemoryUtil.memAlloc(width * height * 4);

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
            IOUtils.createOrGetFile(outputPath);
            IOUtils.writeImage(outputPath, img);

            //unbind texture
            glBindTexture(GL_TEXTURE_2D, 0);

            MemoryUtil.memFree(buffer);
            LOGGER.info("Exported texture to \"%s\"", outputPath.getFileName());

            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to save texture!", e);
        }

        return false;
    }

    /**
     * Loads an image from the given {@link Resource} and returns an {@link ImageData} object containing the image data
     * @param resource The {@link Resource} to load the image from
     * @return An {@link ImageData} object containing the image data
     * @throws Exception if the image could not be loaded
     * @see #load(Resource, boolean, int)
     * @see #load(AITexture)
     * @see #load(AITexture, boolean, int)
     */
    public static ImageData load(Resource resource) throws Exception {
        return load(resource, false, 4);
    }

    /**
     * Loads an image from the given {@link Resource} and returns an {@link ImageData} object containing the image data
     * @param resource The {@link Resource} to load the image from
     * @param flip Whether to flip the image vertically
     * @param desiredChannels The desired number of channels in the loaded image (1 for grayscale, 2 for grayscale + alpha, 3 for RGB, 4 for RGBA)
     * @return An {@link ImageData} object containing the image data
     * @throws Exception if the image could not be loaded
     * @see #load(Resource)
     * @see #load(AITexture)
     * @see #load(AITexture, boolean, int)
     */
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

    /**
     * Loads an image from the given Assimp {@link AITexture} and returns an {@link ImageData} object containing the image data
     * @param texture The Assimp {@link AITexture} to load the image from
     * @return An {@link ImageData} object containing the image data
     * @throws Exception if the image could not be loaded
     * @see #load(AITexture, boolean, int)
     * @see #load(Resource)
     * @see #load(Resource, boolean, int)
     */
    public static ImageData load(AITexture texture) throws Exception {
        return load(texture, false, 4);
    }

    /**
     * Loads an image from the given Assimp {@link AITexture} and returns an {@link ImageData} object containing the image data
     * @param texture The Assimp {@link AITexture} to load the image from
     * @param flip Whether to flip the image vertically
     * @param desiredChannels The desired number of channels in the loaded image (1 for grayscale, 2 for grayscale + alpha, 3 for RGB, 4 for RGBA)
     * @return An {@link ImageData} object containing the image data
     * @throws Exception if the image could not be loaded
     * @see #load(AITexture)
     * @see #load(Resource)
     * @see #load(Resource, boolean, int)
     */
    public static ImageData load(AITexture texture, boolean flip, int desiredChannels) throws Exception {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            STBImage.stbi_set_flip_vertically_on_load(flip);
            ByteBuffer imageBuffer = texture.pcDataCompressed();
            ByteBuffer buffer = STBImage.stbi_load_from_memory(imageBuffer, w, h, channels, desiredChannels);
            STBImage.stbi_set_flip_vertically_on_load(false);

            if (buffer == null) {
                try (AIString name = texture.mFilename()) {
                    throw new Exception("Failed to load image \"" + name.dataString() + "\", " + STBImage.stbi_failure_reason());
                }
            }

            return new ImageData(w.get(), h.get(), buffer);
        }
    }

    /**
     * A simple data class that holds the {@link #width}, {@link #height}, and {@link #buffer} of a loaded image<br>
     * Implements {@link AutoCloseable} to free the image buffer when done
     */
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
