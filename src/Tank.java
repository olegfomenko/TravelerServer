import java.net.InetAddress;
import java.util.ArrayList;

public class Tank extends GameObject {
    private InetAddress address;
    private int port;
    private static int last = 0;
    private int id;
    private ArrayList<Wall> walls;
    private ArrayList<Ball> balls;

    private long ball_delta;

    public Tank(InetAddress address, int port, ArrayList<Wall> walls, ArrayList<Ball> balls) {
        super((float)(1100 + Math.random() * 800), (float)(1100 + Math.random() * 800), 64, 64, (int)(1 + Math.random() * 4), 120);
        this.address = address;
        this.port = port;
        this.walls = walls;
        this.balls = balls;

        id = last++;
    }

    public int getPort() {
        return port;
    }

    public int getId() {
        return id;
    }

    public InetAddress getAddress() {
        return address;
    }

    public long getBall_delta() {
        return  ball_delta;
    }

    public synchronized void update() {
        for (Wall w : walls) if(w.check(this)) {
            switch (getDirection()) {
                case 1: setDirection(2); break;
                case 2: setDirection(1); break;
                case 3: setDirection(4); break;
                case 4: setDirection(3); break;
            }
            break;
        }

        super.update();

        ball_delta += getDt();

        if(ball_delta >= Ball.DELTA) {
            ball_delta = 0;

            synchronized (balls) {
                switch (getDirection()) {
                    case 1: balls.add(new Ball(getX() + getWidth() / 2 - 8, getY() + getHeight() + 10, 1, walls)); break;
                    case 2: balls.add(new Ball(getX() + getWidth() / 2 - 8, getY() - 10 - 16, 2, walls)); break;
                    case 3: balls.add(new Ball(getX() - 10 - 16, getY() + getHeight() / 2 - 8, 3, walls)); break;
                    case 4: balls.add(new Ball(getX() + getWidth() + 10, getY() + getHeight() / 2 - 8, 4, walls)); break;
                }
            }
        }
    }
}
