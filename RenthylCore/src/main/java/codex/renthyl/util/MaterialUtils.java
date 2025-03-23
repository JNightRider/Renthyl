package codex.renthyl.util;

import com.jme3.material.MatParam;
import com.jme3.material.Material;

import java.util.Collection;

public class MaterialUtils {

    /**
     * Transfers values of similarly-named parameters from {@code source} to {@code target}.
     * If a parameter in {@code source} is not set, the similar parameter in {@code target}
     * is {@link Material#clearParam(String) cleared}, if it exists.
     * <p>If {@code overrides} is not null, contained parameters are not transfered. Parameters
     * in {@code target} that have no corresponding parameter in {@code source} are not affected.</p>
     *
     * @param source material to transfer parameter values from
     * @param target material to transfer parameter values to
     * @param overrides if not null, specifies specific parameters in {@code target} that should be conserved
     */
    public static void transferMaterial(Material source, Material target, Collection<String> overrides) {
        for (MatParam p : target.getParams()) {
            if ((overrides == null || !overrides.contains(p.getName()))
                    && source.getMaterialDef().getMaterialParam(p.getName()) != null) {
                MatParam s = source.getParam(p.getName());
                if (s != null) {
                    // the parameter exists in the matdef and is set (by default or by user, does not matter)
                    target.setParam(s.getName(), s.getVarType(), s.getValue());
                } else {
                    // the parameter exists in the matdef, but is not set
                    target.clearParam(p.getName());
                }
            }
        }
    }

    /**
     *
     * @param source
     * @param target
     * @see #transferMaterial(Material, Material, Collection)
     */
    public static void transferMaterial(Material source, Material target) {
        transferMaterial(source, target, null);
    }

}
