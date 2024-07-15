package cinnamon.render;

import org.joml.Matrix4f;

public class Frustum {

    //frustum box planes
    private float
            nxX, nxY, nxZ, nxW,
            nyX, nyY, nyZ, nyW,
            pxX, pxY, pxZ, pxW,
            pyX, pyY, pyZ, pyW;

    //update the frustum box from the camera MVP matrix
    public void updateFrustum(Matrix4f m) {
        nxX = m.m03() + m.m00();
        nxY = m.m13() + m.m10();
        nxZ = m.m23() + m.m20();
        nxW = m.m33() + m.m30();
        pxX = m.m03() - m.m00();
        pxY = m.m13() - m.m10();
        pxZ = m.m23() - m.m20();
        pxW = m.m33() - m.m30();
        nyX = m.m03() + m.m01();
        nyY = m.m13() + m.m11();
        nyZ = m.m23() + m.m21();
        nyW = m.m33() + m.m31();
        pyX = m.m03() - m.m01();
        pyY = m.m13() - m.m11();
        pyZ = m.m23() - m.m21();
        pyW = m.m33() - m.m31();
    }

    //check if a point is culled by the frustum without near-far check
    public boolean culledXY(float x, float y, float z) {
        return  nxX * x + nxY * y + nxZ * z < -nxW ||
                pxX * x + pxY * y + pxZ * z < -pxW ||
                nyX * x + nyY * y + nyZ * z < -nyW ||
                pyX * x + pyY * y + pyZ * z < -pyW;
    }

    //check if a box is culled by the frustum without near-far check
    public boolean culledXY(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return  nxX * (nxX < 0 ? minX : maxX) + nxY * (nxY < 0 ? minY : maxY) + nxZ * (nxZ < 0 ? minZ : maxZ) < -nxW ||
                pxX * (pxX < 0 ? minX : maxX) + pxY * (pxY < 0 ? minY : maxY) + pxZ * (pxZ < 0 ? minZ : maxZ) < -pxW ||
                nyX * (nyX < 0 ? minX : maxX) + nyY * (nyY < 0 ? minY : maxY) + nyZ * (nyZ < 0 ? minZ : maxZ) < -nyW ||
                pyX * (pyX < 0 ? minX : maxX) + pyY * (pyY < 0 ? minY : maxY) + pyZ * (pyZ < 0 ? minZ : maxZ) < -pyW;
    }
}
