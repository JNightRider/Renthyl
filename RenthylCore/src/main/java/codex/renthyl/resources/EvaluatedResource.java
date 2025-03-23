package codex.renthyl.resources;

/**
 * Tracks the resource that most closely matches a {@link codex.renthyl.definitions.ResourceDef ResourceDef}.
 */
public class EvaluatedResource {

    private RenderObject resource;
    private float factor = Float.MAX_VALUE;

    public boolean add(RenderObject resource, float factor) {
        if (resource != null && factor <= this.factor) {
            this.resource = resource;
            this.factor = factor;
            return true;
        }
        return false;
    }

    public boolean isFinal() {
        return factor <= 0 && !isNull();
    }

    public boolean isNull() {
        return resource != null;
    }

    public RenderObject getResource() {
        return resource;
    }

}
