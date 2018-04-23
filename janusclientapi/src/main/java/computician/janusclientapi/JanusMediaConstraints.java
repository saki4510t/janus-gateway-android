package computician.janusclientapi;

/**
 * Created by ben.trent on 6/25/2015.
 */
public class JanusMediaConstraints {

    public class JanusVideo {
        private int maxHeight, minHeight, maxWidth, minWidth, maxFramerate, minFramerate;

        public JanusVideo() {
            maxFramerate = 15;
            minFramerate = 0;
            maxHeight = 240;
            minHeight = 0;
            maxWidth = 320;
            minWidth = 0;
        }

        public int getMaxHeight() {
            return maxHeight;
        }

        public void setMaxHeight(final int maxHeight) {
            this.maxHeight = maxHeight;
        }

        public int getMinHeight() {
            return minHeight;
        }

        public void setMinHeight(final int minHeight) {
            this.minHeight = minHeight;
        }

        public int getMaxWidth() {
            return maxWidth;
        }

        public void setMaxWidth(final int maxWidth) {
            this.maxWidth = maxWidth;
        }

        public int getMinWidth() {
            return minWidth;
        }

        public void setMinWidth(final int minWidth) {
            this.minWidth = minWidth;
        }

        public int getMaxFramerate() {
            return maxFramerate;
        }

        public void setMaxFramerate(final int maxFramerate) {
            this.maxFramerate = maxFramerate;
        }

        public int getMinFramerate() {
            return minFramerate;
        }

        public void setMinFramerate(final int minFramerate) {
            this.minFramerate = minFramerate;
        }
    }

    public enum Camera {
        front,
        back
    }

    private boolean sendAudio = true;
    private JanusVideo video = new JanusVideo();
    private boolean recvVideo = true;
    private boolean recvAudio = true;
    private Camera camera = Camera.front;

    public JanusMediaConstraints() {
    }

    public JanusVideo getVideo() {
        return video;
    }

    public Boolean getSendVideo() {
        return video != null;
    }

    public void setVideo(final JanusVideo video) {
        this.video = video;
    }

    public boolean getSendAudio() {
        return sendAudio;
    }

    public void setSendAudio(final boolean sendAudio) {
        this.sendAudio = sendAudio;
    }

    public void setRecvVideo(final boolean recvVideo) {
        this.recvVideo = recvVideo;
    }

    public boolean getRecvVideo() {
        return recvVideo;
    }

    public void setRecvAudio(final boolean recvAudio) {
        this.recvAudio = recvAudio;
    }

    public boolean getRecvAudio() {
        return recvAudio;
    }

//    public Camera getCamera() {
//        // FIXME should remove
//        return camera;
//    }
//
//    public void setCamera(final Camera camera) {
//        // FIXME should remove
//        this.camera = camera;
//    }

}
