package cinnamon.gui.screens.extras;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.Toast;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.Slider;
import cinnamon.model.GeometryHelper;
import cinnamon.model.SimpleGeometry;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.shader.PostProcess;
import cinnamon.render.shader.Shader;
import cinnamon.render.texture.Texture;
import cinnamon.text.Text;
import cinnamon.utils.FileDialog;
import cinnamon.utils.Resource;
import cinnamon.utils.TextureIO;

import java.nio.file.Path;

import static cinnamon.Client.LOGGER;

public class HDRFixScreen extends ParentedScreen {

    private static final Framebuffer framebuffer = new Framebuffer(1, 1, Framebuffer.COLOR_BUFFER);
    private static final Resource
            OPEN_TEX = new Resource("textures/gui/icons/open.png"),
            CLOSE_TEX = new Resource("textures/gui/icons/close.png"),
            SAVE_TEX = new Resource("textures/gui/icons/save.png");

    private Resource imagePath;
    private int w, h;
    private boolean hasImage;

    private final Slider gamma = new Slider(0, 0, 200);

    public HDRFixScreen(Screen parentScreen) {
        super(parentScreen);
        gamma.setPercentage(0.22f);
        gamma.setTooltipFunction((f, i) -> Text.translated("gui.hdr_fix_screen.gamma", String.format("%.2f", f * 10f), "2.20"));
    }

    @Override
    public void init() {
        Button save = new Button(4 + 16, 4, 16, 16, null, button -> {
            String file = FileDialog.saveFile(new FileDialog.Filter("png image", "png"));
            if (file != null) {
                try {
                    TextureIO.saveTexture(framebuffer.getColorBuffer(), Path.of(file), false, false);
                    Toast.addToast(Text.translated("gui.hdr_fix_screen.saved")).type(Toast.ToastType.SUCCESS);
                } catch (Exception e) {
                    Toast.addToast(Text.translated("gui.hdr_fix_screen.error.save")).type(Toast.ToastType.ERROR);
                    LOGGER.error("Failed to save image: " + imagePath, e);
                }
            }
        });
        save.setActive(hasImage);
        save.setIcon(SAVE_TEX);
        save.setTooltip(Text.translated("gui.save"));

        Button openButton = new Button(4, 4, 16, 16, null, button -> {
            String file = FileDialog.openFile(FileDialog.Filter.IMAGE_FILES);
            if (file != null) {
                imagePath = new Resource("", file);
                try (TextureIO.ImageData imageData = TextureIO.load(imagePath)) {
                    w = imageData.width;
                    h = imageData.height;
                    hasImage = true;
                    save.setActive(true);
                } catch (Exception e) {
                    Toast.addToast(Text.translated("gui.hdr_fix_screen.error.load")).type(Toast.ToastType.ERROR);
                    LOGGER.error("Failed to load image: " + imagePath, e);
                }
            }
        });
        openButton.setIcon(OPEN_TEX);
        openButton.setTooltip(Text.translated("gui.open"));
        addWidget(openButton);

        addWidget(save);

        gamma.setPos(width / 2 - 100, 4 + 4);
        addWidget(gamma);

        super.init();
    }

    @Override
    protected void addBackButton() {
        //super.addBackButton();

        //close button
        Button closeButton = new Button(width - 4 - 16, 4, 16, 16, null, button -> close());
        closeButton.setIcon(CLOSE_TEX);
        closeButton.setTooltip(Text.translated("gui.close"));
        addWidget(closeButton);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        if (!hasImage)
            return;

        //calculate max size to fit in the screen
        int maxWidth = width - 8;
        int maxHeight = height - 8 - 20;
        float aspectRatio = (float) w / h;
        float imgWidth = Math.min(maxWidth, maxHeight * aspectRatio);
        float imgHeight = Math.min(maxHeight, maxWidth / aspectRatio);

        renderImage();
        VertexConsumer.MAIN.consume(
                GeometryHelper.quad(matrices, (width - imgWidth) / 2f, (height - imgHeight) / 2f + 10, imgWidth, imgHeight, 0, 0, w, h, w, h),
                framebuffer.getColorBuffer()
        );
    }

    private void renderImage() {
        Framebuffer oldFB = Framebuffer.activeFramebuffer;
        Shader oldSh = Shader.activeShader;

        framebuffer.resize(w, h);
        framebuffer.useClear();
        framebuffer.adjustViewPort();

        Shader shader = PostProcess.BLIT_GAMMA.getShader().use();
        shader.setTexture("colorTex", Texture.of(imagePath, Texture.TextureParams.SMOOTH_SAMPLING), 0);
        shader.setFloat("gamma", gamma.getPercentage() * 10f);

        SimpleGeometry.QUAD.render();

        Texture.unbindTex(0);

        oldSh.use();
        oldFB.use();
        oldFB.adjustViewPort();
    }
}
