/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.1
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package org.pjsip.pjsua2;

public class VidDevManager {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected VidDevManager(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(VidDevManager obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        throw new UnsupportedOperationException("C++ destructor does not have public access");
      }
      swigCPtr = 0;
    }
  }

  public void refreshDevs() throws Exception {
    pjsua2JNI.VidDevManager_refreshDevs(swigCPtr, this);
  }

  public long getDevCount() {
    return pjsua2JNI.VidDevManager_getDevCount(swigCPtr, this);
  }

  public VideoDevInfo getDevInfo(int dev_id) throws Exception {
    return new VideoDevInfo(pjsua2JNI.VidDevManager_getDevInfo(swigCPtr, this, dev_id), true);
  }

  public VideoDevInfoVector2 enumDev2() throws Exception {
    return new VideoDevInfoVector2(pjsua2JNI.VidDevManager_enumDev2(swigCPtr, this), true);
  }

  public int lookupDev(String drv_name, String dev_name) throws Exception {
    return pjsua2JNI.VidDevManager_lookupDev(swigCPtr, this, drv_name, dev_name);
  }

  public String capName(int cap) {
    return pjsua2JNI.VidDevManager_capName(swigCPtr, this, cap);
  }

  public void setFormat(int dev_id, MediaFormatVideo format, boolean keep) throws Exception {
    pjsua2JNI.VidDevManager_setFormat(swigCPtr, this, dev_id, MediaFormatVideo.getCPtr(format), format, keep);
  }

  public MediaFormatVideo getFormat(int dev_id) throws Exception {
    return new MediaFormatVideo(pjsua2JNI.VidDevManager_getFormat(swigCPtr, this, dev_id), true);
  }

  public void setInputScale(int dev_id, MediaSize scale, boolean keep) throws Exception {
    pjsua2JNI.VidDevManager_setInputScale(swigCPtr, this, dev_id, MediaSize.getCPtr(scale), scale, keep);
  }

  public MediaSize getInputScale(int dev_id) throws Exception {
    return new MediaSize(pjsua2JNI.VidDevManager_getInputScale(swigCPtr, this, dev_id), true);
  }

  public void setOutputWindowFlags(int dev_id, int flags, boolean keep) throws Exception {
    pjsua2JNI.VidDevManager_setOutputWindowFlags(swigCPtr, this, dev_id, flags, keep);
  }

  public int getOutputWindowFlags(int dev_id) throws Exception {
    return pjsua2JNI.VidDevManager_getOutputWindowFlags(swigCPtr, this, dev_id);
  }

  public void switchDev(int dev_id, VideoSwitchParam param) throws Exception {
    pjsua2JNI.VidDevManager_switchDev(swigCPtr, this, dev_id, VideoSwitchParam.getCPtr(param), param);
  }

  public boolean isCaptureActive(int dev_id) {
    return pjsua2JNI.VidDevManager_isCaptureActive(swigCPtr, this, dev_id);
  }

  public void setCaptureOrient(int dev_id, int orient, boolean keep) throws Exception {
    pjsua2JNI.VidDevManager_setCaptureOrient__SWIG_0(swigCPtr, this, dev_id, orient, keep);
  }

  public void setCaptureOrient(int dev_id, int orient) throws Exception {
    pjsua2JNI.VidDevManager_setCaptureOrient__SWIG_1(swigCPtr, this, dev_id, orient);
  }

}
