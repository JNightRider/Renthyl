# Shadows

RenthylPlus provides support for shadow calculations through  ShadowOcclusionPass and ShadowComposerPass. The former generates shadow maps for a light source, and the latter composes shadow maps into one light contribution texture based on the current camera view.

### ShadowOcclusionPass

Three extensions are provided of ShadowOcclusionPass: DirectionalShadowPass for DirectionalLights, PointShadowPass for PointLights, and SpotShadowPass for SpotLights. All three behave similarly from an API perspective, so they are likewise used similarly