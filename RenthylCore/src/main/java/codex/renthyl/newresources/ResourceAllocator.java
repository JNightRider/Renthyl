package codex.renthyl.newresources;

import codex.renthyl.definitions.ResourceDef;

public interface ResourceAllocator <T extends ResourceWrapper> {

    T allocate(ResourceDef def, int start, int end);

    void flush();

}
