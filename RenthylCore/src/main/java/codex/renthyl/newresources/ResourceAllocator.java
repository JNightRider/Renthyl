package codex.renthyl.newresources;

import codex.renthyl.definitions.ResourceDef;

public interface ResourceAllocator <T extends AllocatedResource> {

    T allocate(ResourceDef def, int start, int end);

}
