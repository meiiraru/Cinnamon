package mayo.render;

import mayo.model.Renderable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BatchRenderer {
    private final Map<Shaders, List<Batch>> batches = new HashMap<>();

    public void addElement(Shaders shader, Renderable renderable) {
        List<Batch> batchList = batches.computeIfAbsent(shader, k -> new ArrayList<>());

        for (Batch batch : batchList) {
            if (!batch.isFilled() && (renderable.textureID > -1 || batch.hasTextureSpace() || batch.hasTexture(renderable.textureID))) {
                batch.addElement(renderable);
                return;
            }
        }

        Batch batch = new Batch(shader.getShader());
        batchList.add(batch);
        batch.addElement(renderable);
    }

    public void removeElement(Renderable renderable) {
        for (List<Batch> list : batches.values()) {
            for (Batch batch : list) {
                if (batch.removeElement(renderable))
                    return;
            }
        }
    }

    public void render() {
        for (List<Batch> list : batches.values()) {
            for (Batch batch : list) {
                batch.render();
            }
        }
    }
}
