package codex.renthyl.newresources;

import codex.renthyl.FGRenderContext;

public interface Executable {

    void prepare();

    boolean ready();

    boolean claim(Worker worker);

    void execute(FGRenderContext context);

    boolean isComplete();

    void reset();

}
