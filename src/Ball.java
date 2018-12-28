import java.util.ArrayList;

public class Ball extends GameObject {
    private ArrayList<Wall> walls;
    private int id;
    public static final long DELTA = 5000;
    private static int last = 0;

    public Ball(float x, float y, int direction, ArrayList<Wall> walls) {
        super(x, y, 15, 15, direction,180);
        this.walls = walls;
        id = last++;
    }

    public int getId() {
        return id;
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
    }
}