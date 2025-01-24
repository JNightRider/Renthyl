/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.plex;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GL45;

/**
 *
 * @author codex
 */
public class PlexMesh extends Mesh {
    
    private FloatBuffer metrics;
    private int rasterLimit = -1;
    private final Vector3f tempPoint1 = new Vector3f();
    private final Vector3f tempPoint2 = new Vector3f();
    private final Vector3f tempPoint3 = new Vector3f();
    
    public PlexMesh(Mesh mesh) {
        for (VertexBuffer v : mesh.getBufferList()) {
            setBuffer(v.clone());
        }
        sortIndexBuffer();
    }
    
    @Override
    public int getVertexCount() {
        return rasterLimit / getBuffer(VertexBuffer.Type.Index).getNumComponents();
    }
    
    /**
     * Sorts the mesh's index buffer descending according to the surface area of
     * the associated triangles.
     */
    private void sortIndexBuffer() {
        IntBuffer indices = (IntBuffer)getBuffer(VertexBuffer.Type.Index).getData();
        FloatBuffer verts = getFloatBuffer(VertexBuffer.Type.Position);
        int numTris = indices.limit() / 3;
        // unpack vertices into a sortable form
        List<MeshTriangle> triangles = new ArrayList<>(numTris);
        for (int i = 0; i < indices.limit(); i++) {
            int v1 = indices.get(i);
            int v2 = indices.get(i + 1);
            int v3 = indices.get(i + 2);
            readVector(verts, v1, tempPoint1);
            readVector(verts, v2, tempPoint2);
            readVector(verts, v3, tempPoint3);
            triangles.add(new MeshTriangle(calcTriangleArea(tempPoint1, tempPoint2, tempPoint3), v1, v2, v3));
        }
        // create the areas buffer, which will help us choose the correct
        // position for the index buffer limit
        if (metrics == null || metrics.capacity() < numTris) {
            metrics = BufferUtils.createFloatBuffer(numTris);
        } else {
            metrics.rewind();
        }
        // stream and sort the triangle list in descending order, then repack
        indices.rewind();
        triangles.stream().sorted((o1, o2) -> {
            if (o1.area < o2.area) return 1;
            else if (o1.area > o2.area) return -1;
            else return 0;
        }).forEachOrdered(t -> {
            indices.put(t.v1).put(t.v2).put(t.v3);
            // store the width of a square equal to the triangle in area
            metrics.put(FastMath.sqrt(t.area));
        });
        metrics.flip();
        indices.rewind();
        getBuffer(VertexBuffer.Type.Index).updateData(indices);
    }
    private void readVector(FloatBuffer verts, int index, Vector3f store) {
        index *= 3;
        store.x = verts.get(index);
        store.y = verts.get(index + 1);
        store.z = verts.get(index + 2);
    }
    private float calcTriangleArea(Vector3f p1, Vector3f p2, Vector3f p3) {
        
        // For this calculation, p1 will act as a temporary vector, as we would
        // move p1 to be at (0, 0, 0) anyway.
        
        // set p2 as a vector pointing from p1 to p2
        p2.subtractLocal(p1);
        // set p3 as a vector pointing from p1 to p3
        p3.subtractLocal(p1);
        // set p1 vector perpedicular to p2 and p3 (resulting length does not matter)
        p2.cross(p3, p1);
        // set p1 perpedicular to p1 and p2 then normalize
        p1.crossLocal(p2).normalizeLocal();
        // project p3 onto p1 to create a rectangle formed by p1 and p2 where half
        // its area is the area of the triangle
        p1.multLocal(FastMath.abs(p1.dot(p3)));
        return FastMath.sqrt(p1.lengthSquared() * p2.lengthSquared()) * 0.5f;
        
    }
    
    public int selectRasterLimit(Camera cam, float distance) {
        if (metrics == null) {
            throw new NullPointerException("Mesh metrics have not been calculated.");
        }
        VertexBuffer ivb = getBuffer(VertexBuffer.Type.Index);
        IntBuffer indices = (IntBuffer)ivb.getData().clear();
        float threshold = FastMath.sqr(1f);
        int[] range = {0, indices.limit()-1}; // available lod range (inclusive)
        if (rasterLimit < range[0] || rasterLimit > range[1]) {
            rasterLimit = (range[1] + range[0]) >> 1;
        }
        for (int i = 0;; i++) {
            if (i > 10000) {
                // this may save me one day
                throw new RuntimeException();
            }
            if (getViewportMetric(cam, distance, rasterLimit) > threshold) {
                // add one because the LOD index specifies where to begin
                // software rendering triangles
                range[0] = ++rasterLimit;
            } else {
                range[1] = rasterLimit;
            }
            if (range[1] - range[0] <= 0) {
                break;
            }
            rasterLimit = (range[1] + range[0]) >> 1;
        }
        indices.limit(rasterLimit);
        ivb.updateData(indices);
        return rasterLimit;
    }
    private float getViewportMetric(Camera cam, float distance, int index) {
        // create a right isometric triangle
        float metric = metrics.get(index);
        tempPoint1.set(metric, 0f, distance);
        tempPoint2.set(0f, metric, distance);
        // transform the triangle to screen space
        cam.getProjectionMatrix().mult(tempPoint1, tempPoint1);
        cam.getProjectionMatrix().mult(tempPoint2, tempPoint2);
        // return the area of the rectangle formed by the triangle squared
        return tempPoint1.x * tempPoint2.y * cam.getWidth() * cam.getHeight();
    }
    
    public int getRasterLimit() {
        return rasterLimit;
    }
    
    private static class MeshTriangle {
        
        public final float area;
        public final int v1, v2, v3;
        
        public MeshTriangle(float area, int v1, int v2, int v3) {
            this.area = area;
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
        }
        
    }
    
}
