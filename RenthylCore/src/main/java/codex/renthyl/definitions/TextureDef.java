/*
 * Copyright (c) 2024, codex
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package codex.renthyl.definitions;

import codex.boost.export.SavableObject;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture3D;
import com.jme3.texture.image.ColorSpace;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * General resource definition for textures.
 * <p>
 * Able to indirectly reallocate any object that contains a locatable {@link Image} that
 * matches the properties given in this definition.
 * 
 * @author codex
 * @param <T>
 */
public class TextureDef <T extends Texture> extends AbstractResourceDef<T> {
    
    public static final Function<Image, Texture2D> TEXTURE_2D = img -> new Texture2D(img);
    public static final Function<Image, Texture3D> TEXTURE_3D = img -> new Texture3D(img);
    
    private final Class<T> type;
    private Function<Image, T> textureBuilder;
    private int width = 128;
    private int height = 128;
    private int depth = 0;
    private int samples = 1;
    private Image.Format format;
    private ColorSpace colorSpace = ColorSpace.Linear;
    private Texture.MagFilter magFilter = Texture.MagFilter.Bilinear;
    private Texture.MinFilter minFilter = Texture.MinFilter.BilinearNoMipMaps;
    private Texture.ShadowCompareMode shadowCompare = Texture.ShadowCompareMode.Off;
    private Texture.WrapMode wrapS, wrapT, wrapR;
    private boolean formatFlexible = false;
    private boolean colorSpaceFlexible = false;
    
    /**
     * 
     * @param type
     * @param textureBuilder 
     */
    public TextureDef(Class<T> type, Function<Image, T> textureBuilder) {
        this(type, textureBuilder, Image.Format.RGBA8);
    }
    /**
     * 
     * @param type
     * @param textureBuilder
     * @param format 
     */
    public TextureDef(Class<T> type, Function<Image, T> textureBuilder, Image.Format format) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(textureBuilder);
        Objects.requireNonNull(format);
        this.type = type;
        this.textureBuilder = textureBuilder;
        this.format = format;
        if (!Texture2D.class.isAssignableFrom(type)) {
            depth = 1;
        }
    }
    
    @Override
    public T createResource() {
        Image img;
        if (depth > 0) {
            ArrayList<ByteBuffer> data = new ArrayList<>(1);
            img = new Image(format, width, height, depth, data, colorSpace);
        } else {
            img = new Image(format, width, height, null, colorSpace);
        }
        return createTexture(img);
    }
    @Override
    public float evaluateResource(Object resource) {
        if (type.isAssignableFrom(resource.getClass())) {
            T tex = (T)resource;
            if (validateImage(tex.getImage())) {
                return 0;
            }
        } else if (resource instanceof Texture) {
            if (validateImage(((Texture)resource).getImage())) {
                return 1;
            }
        } else if (resource instanceof Image) {
            if (validateImage((Image)resource)) {
                return 1;
            }
        }
        return Float.POSITIVE_INFINITY;
    }

    @Override
    public T applyResource(Object resource) {
        if (type.isAssignableFrom(resource.getClass())) {
            T tex = (T)resource;
            setupTexture(tex);
            return tex;
        }
        Image img;
        if (resource instanceof Texture) {
            img = ((Texture)resource).getImage();
        } else if (resource instanceof Image) {
            img = (Image)resource;
        } else {
            throw new IllegalStateException("Image not found.");
        }
        T tex = textureBuilder.apply(img);
        setupTexture(tex);
        return tex;
    }
    @Override
    public Consumer<T> getDisposalMethod() {
        return tex -> tex.getImage().dispose();
    }
    @Override
    public boolean isDisposeOnRelease() {
        return false;
    }
    
    protected T createTexture(Image img) {
        T tex = textureBuilder.apply(img);
        tex.getImage().setMultiSamples(samples);
        setupTexture(tex);
        return tex;
    }
    protected void setupTexture(Texture tex) {
        if (magFilter != null) {
            tex.setMagFilter(magFilter);
        }
        if (minFilter != null) {
            tex.setMinFilter(minFilter);
        }
        if (shadowCompare != null) {
            tex.setShadowCompareMode(shadowCompare);
        }
        if (wrapS != null) {
            tex.setWrap(Texture.WrapAxis.S, wrapS);
        }
        if (wrapT != null) {
            tex.setWrap(Texture.WrapAxis.T, wrapT);
        }
        if (wrapR != null && depth > 0) {
            tex.setWrap(Texture.WrapAxis.R, wrapR);
        }
    }
    protected boolean validateImage(Image img) {
        return validateImageSize(img)
            && (samples <= 0 || img.getMultiSamples() == samples)
            && validateImageFormat(img)
            && (colorSpaceFlexible || img.getColorSpace() == colorSpace);
    }
    protected boolean validateImageSize(Image img) {
        return img.getWidth() == width && img.getHeight() == height && (depth == 0 || img.getDepth() == depth);
    }
    protected boolean validateImageFormat(Image img) {
        return img.getFormat() == format || (formatFlexible && img.getFormat().isDepthFormat() == format.isDepthFormat());
    }
    
    /**
     * Sets the function that constructs a texture from an image.
     * 
     * @param textureBuilder 
     */
    public void setTextureBuilder(Function<Image, T> textureBuilder) {
        this.textureBuilder = Objects.requireNonNull(textureBuilder);
    }
    /**
     * Sets the texture width.
     * 
     * @param width texture width greater than zero
     */
    public void setWidth(int width) {
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be greater than zero.");
        }
        this.width = width;
    }
    /**
     * Sets the texture height.
     * 
     * @param height texture height greater than zero
     */
    public void setHeight(int height) {
        if (height <= 0) {
            throw new IllegalArgumentException("Height must be greater than zero.");
        }
        this.height = height;
    }
    /**
     * Sets the texture depth.
     * <p>
     * Values less than or equal to zero indicate a 2D texture.
     * 
     * @param depth texture depth (or less or equal to than zero)
     */
    public void setDepth(int depth) {
        if (depth < 0) {
            throw new IllegalArgumentException("Depth cannot be less than zero.");
        }
        this.depth = depth;
    }
    /**
     * Sets the width and height of the texture to the length.
     * 
     * @param length 
     */
    public void setSquare(int length) {
        assert length > 0 : "Length must be more than zero.";
        width = height = length;
    }
    /**
     * Sets the width, height, and depth of the texture to the length.
     * 
     * @param length 
     */
    public void setCube(int length) {
        assert length > 0 : "Length must be more than zero.";
        width = height = depth = length;
    }
    /**
     * Sets the width and height of the texture.
     * 
     * @param width
     * @param height 
     */
    public void setSize(int width, int height) {
        setWidth(width);
        setHeight(height);
    }
    /**
     * Sets the width, height, and depth of the texture.
     * 
     * @param width
     * @param height
     * @param depth 
     */
    public void setSize(int width, int height, int depth) {
        setWidth(width);
        setHeight(height);
        setDepth(depth);
    }
    /**
     * Sets the width, height, and depth of the texture from the given definition.
     * 
     * @param def 
     */
    public void setSize(TextureDef<T> def) {
        setWidth(def.width);
        setHeight(def.height);
        setDepth(def.depth);
    }
    /**
     * Sets the given texture demensions to contain at least the specified number of pixels.
     * 
     * @param pixels
     * @param w true to add width
     * @param h true to add height
     * @param d true to add depth
     */
    public void setNumPixels(int pixels, boolean w, boolean h, boolean d) {
        int n = 0;
        if (w) n++;
        if (h) n++;
        if (d) n++;
        int length;
        switch (n) {
            case 3:  length = (int)Math.ceil(Math.cbrt(pixels)); break;
            case 2:  length = (int)Math.ceil(Math.sqrt(pixels)); break;
            default: length = pixels;
        }
        if (w) width = length;
        if (h) height = length;
        if (d) depth = length;
    }
    /**
     * Sets the number of samples of the texture's image.
     * 
     * @param samples 
     */
    public void setSamples(int samples) {
        if (samples <= 0) {
            throw new IllegalArgumentException("Image samples must be greater than zero.");
        }
        this.samples = samples;
    }
    /**
     * Sets the format of the image.
     * 
     * @param format 
     */
    public void setFormat(Image.Format format) {
        Objects.requireNonNull(format);
        this.format = format;
    }
    /**
     * Sets reallocation so that the target image only needs to have the same
     * format type (color versus depth) as this definition.
     * <p>
     * default=false
     * 
     * @param formatFlexible 
     */
    public void setFormatFlexible(boolean formatFlexible) {
        this.formatFlexible = formatFlexible;
    }
    /**
     * Sets the color space of the texture.
     * 
     * @param colorSpace 
     */
    public void setColorSpace(ColorSpace colorSpace) {
        this.colorSpace = colorSpace;
    }
    /**
     * Sets the magnification filter of the texture.
     * 
     * @param magFilter mag filter, or null to use default
     */
    public void setMagFilter(Texture.MagFilter magFilter) {
        this.magFilter = magFilter;
    }
    /**
     * Sets the minification filter of the texture.
     * 
     * @param minFilter min filter, or null to use default
     */
    public void setMinFilter(Texture.MinFilter minFilter) {
        this.minFilter = minFilter;
    }
    /**
     * Sets the shadow compare mode.
     * 
     * @param shadowCompare 
     */
    public void setShadowCompare(Texture.ShadowCompareMode shadowCompare) {
        this.shadowCompare = shadowCompare;
    }
    /**
     * Sets reallocation so that the target image does not need the same
     * color space as this definition.
     * 
     * @param colorSpaceFlexible 
     */
    public void setColorSpaceFlexible(boolean colorSpaceFlexible) {
        this.colorSpaceFlexible = colorSpaceFlexible;
    }
    /**
     * Sets the wrap mode on all axis.
     * 
     * @param mode 
     */
    public void setWrap(Texture.WrapMode mode) {
        wrapS = wrapT = wrapR = mode;
    }
    /**
     * Sets the wrap mode on the specified axis.
     * 
     * @param axis
     * @param mode 
     */
    public void setWrap(Texture.WrapAxis axis, Texture.WrapMode mode) {
        switch (axis) {
            case S: wrapS = mode; break;
            case T: wrapT = mode; break;
            case R: wrapR = mode; break;
        }
    }
    
    /**
     * Gets the texture type handled by this definition.
     * 
     * @return 
     */
    public Class<T> getType() {
        return type;
    }
    /**
     * 
     * @return 
     */
    public Function<Image, T> getTextureBuilder() {
        return textureBuilder;
    }
    /**
     * 
     * @return 
     */
    public int getWidth() {
        return width;
    }
    /**
     * 
     * @return 
     */
    public int getHeight() {
        return height;
    }
    /**
     * 
     * @return 
     */
    public int getDepth() {
        return depth;
    }
    /**
     * Returns the number of pixels contained in the texture.
     * 
     * @return 
     */
    public int getNumPixels() {
        if (depth > 0) return width*height*depth;
        else return width*height;
    }
    /**
     * 
     * @return 
     */
    public int getSamples() {
        return samples;
    }
    /**
     * 
     * @return 
     */
    public Image.Format getFormat() {
        return format;
    }
    /**
     * 
     * @return 
     */
    public boolean isFormatFlexible() {
        return formatFlexible;
    }
    /**
     * 
     * @return 
     */
    public ColorSpace getColorSpace() {
        return colorSpace;
    }
    /**
     * 
     * @return 
     */
    public Texture.MagFilter getMagFilter() {
        return magFilter;
    }
    /**
     * 
     * @return 
     */
    public Texture.MinFilter getMinFilter() {
        return minFilter;
    }
    /**
     * 
     * @return 
     */
    public Texture.ShadowCompareMode getShadowCompare() {
        return shadowCompare;
    }
    /**
     * 
     * @return 
     */
    public boolean isColorSpaceFlexible() {
        return colorSpaceFlexible;
    }
    /**
     * Gets the wrap mode on the specified axis.
     * 
     * @param axis
     * @return 
     */
    public Texture.WrapMode getWrap(Texture.WrapAxis axis) {
        switch (axis) {
            case S: return wrapS;
            case T: return wrapT;
            case R: return wrapR;
            default: throw new IllegalArgumentException();
        }
    }
    
    /**
     * Creates a general-purpose definition for {@link Texture2D}s.
     * 
     * @return 
     */
    public static TextureDef<Texture2D> texture2D() {
        return new TextureDef<>(Texture2D.class, TEXTURE_2D);
    }
    /**
     * Creates a general-purpose definition for {@link Texture3D}s.
     * 
     * @return 
     */
    public static TextureDef<Texture3D> texture3D() {
        return new TextureDef<>(Texture3D.class, TEXTURE_3D);
    }
    /**
     * Creates a general-purpose definition for {@link Texture2D}s.
     * 
     * @param format
     * @return 
     */
    public static TextureDef<Texture2D> texture2D(Image.Format format) {
        return new TextureDef<>(Texture2D.class, TEXTURE_2D, format);
    }
    /**
     * Creates a general-purpose definition for {@link Texture3D}s.
     * 
     * @param format
     * @return 
     */
    public static TextureDef<Texture3D> texture3D(Image.Format format) {
        return new TextureDef<>(Texture3D.class, TEXTURE_3D, format);
    }
    
    /**
     * Creates a savable 2D texture definition capsule.
     * <p>
     * Texture builders and image extractors are not saved.
     * 
     * @param texDef
     * @return 
     */
    public static Savable saveTexture2D(TextureDef<Texture2D> texDef) {
        return new Texture2DCapsule(texDef);
    }
    /**
     * Creates a savable 3D texture definition capsule.
     * <p>
     * Texture builders and image extractors are not saved.
     * 
     * @param texDef
     * @return 
     */
    public static Savable saveTexture3D(TextureDef<Texture3D> texDef) {
        return new Texture3DCapsule(texDef);
    }
    
    public static TextureDef<Texture2D> readTexture2D(InputCapsule in, String name, TextureDef<Texture2D> defValue) throws IOException {
        return SavableObject.readSavable(in, name, TextureDefCapsule.class, new Texture2DCapsule(defValue)).getTextureDef();
    }
    public static TextureDef<Texture2D> readTexture3D(InputCapsule in, String name, TextureDef<Texture3D> defValue) throws IOException {
        return SavableObject.readSavable(in, name, TextureDefCapsule.class, new Texture3DCapsule(defValue)).getTextureDef();
    }
    
    public static abstract class TextureDefCapsule <T extends Texture> implements Savable {

        protected TextureDef<T> textureDef;

        public TextureDefCapsule(TextureDef<T> textureDef) {
            this.textureDef = textureDef;
        }
        
        @Override
        public void write(JmeExporter ex) throws IOException {
            OutputCapsule out = ex.getCapsule(this);
            out.write(textureDef.getWidth(), "width", 128);
            out.write(textureDef.getHeight(), "height", 128);
            out.write(textureDef.getDepth(), "depth", 1);
            out.write(textureDef.getSamples(), "samples", 1);
            out.write(textureDef.getFormat(), "format", Image.Format.RGBA8);
            out.write(textureDef.isFormatFlexible(), "formatFlexible", false);
            out.write(textureDef.getColorSpace(), "colorSpace", ColorSpace.Linear);
            out.write(textureDef.getMagFilter(), "magFilter", Texture.MagFilter.Bilinear);
            out.write(textureDef.getMinFilter(), "minFilter", Texture.MinFilter.BilinearNoMipMaps);
            out.write(textureDef.getShadowCompare(), "shadowCompare", Texture.ShadowCompareMode.Off);
            out.write(textureDef.isColorSpaceFlexible(), "colorSpaceFlexible", false);
            out.write(textureDef.getWrap(Texture.WrapAxis.R), "wrapR", Texture.WrapMode.EdgeClamp);
            out.write(textureDef.getWrap(Texture.WrapAxis.S), "wrapS", Texture.WrapMode.EdgeClamp);
            out.write(textureDef.getWrap(Texture.WrapAxis.T), "wrapT", Texture.WrapMode.EdgeClamp);
        }
        @Override
        public void read(JmeImporter im) throws IOException {
            InputCapsule in = im.getCapsule(this);
            textureDef = createDefinition();
            textureDef.setWidth(in.readInt("width", 128));
            textureDef.setHeight(in.readInt("height", 128));
            textureDef.setDepth(in.readInt("depth", 1));
            textureDef.setSamples(in.readInt("samples", 1));
            textureDef.setFormat(in.readEnum("format", Image.Format.class, Image.Format.RGBA8));
            textureDef.setFormatFlexible(in.readBoolean("formatFlexible", false));
            textureDef.setColorSpace(in.readEnum("colorSpace", ColorSpace.class, ColorSpace.Linear));
            textureDef.setMagFilter(in.readEnum("magFilter", Texture.MagFilter.class, Texture.MagFilter.Bilinear));
            textureDef.setMinFilter(in.readEnum("minFilter", Texture.MinFilter.class, Texture.MinFilter.BilinearNoMipMaps));
            textureDef.setShadowCompare(in.readEnum("shadowCompare", Texture.ShadowCompareMode.class, Texture.ShadowCompareMode.Off));
            textureDef.setColorSpaceFlexible(in.readBoolean("colorSpaceFlexible", false));
            textureDef.setWrap(Texture.WrapAxis.R, in.readEnum("wrapR", Texture.WrapMode.class, Texture.WrapMode.EdgeClamp));
            textureDef.setWrap(Texture.WrapAxis.S, in.readEnum("wrapS", Texture.WrapMode.class, Texture.WrapMode.EdgeClamp));
            textureDef.setWrap(Texture.WrapAxis.T, in.readEnum("wrapT", Texture.WrapMode.class, Texture.WrapMode.EdgeClamp));
        }
        
        protected abstract TextureDef<T> createDefinition();
        
        public TextureDef<T> getTextureDef() {
            return textureDef;
        }
        
    }
    public static class Texture2DCapsule extends TextureDefCapsule<Texture2D> {
        
        public Texture2DCapsule() {
            this(null);
        }
        public Texture2DCapsule(TextureDef<Texture2D> textureDef) {
            super(textureDef);
        }

        @Override
        protected TextureDef<Texture2D> createDefinition() {
            return texture2D();
        }
        
    }
    public static class Texture3DCapsule extends TextureDefCapsule<Texture3D> {
        
        public Texture3DCapsule() {
            this(null);
        }
        public Texture3DCapsule(TextureDef<Texture3D> textureDef) {
            super(textureDef);
        }

        @Override
        protected TextureDef<Texture3D> createDefinition() {
            return texture3D();
        }
        
    }
    
}
