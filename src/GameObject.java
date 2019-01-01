public class GameObject {
    private volatile float x, y;
    private volatile int direction;
    private volatile float width, height;
    private float speed;
    public long last = System.currentTimeMillis(), cur, dt;

    public GameObject(float x, float y, float width, float height, int direction, float speed) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.direction = direction;
        this.speed = speed;
    }

    public synchronized float getWidth() {
        return width;
    }

    public synchronized  float getHeight() {
        return height;
    }

    public synchronized float getX() {
        return x;
    }

    public synchronized float getY() {
        return y;
    }

    public synchronized int getDirection() {
        return direction;
    }

    public synchronized void setDirection(int direction) {
        this.direction = direction;
    }

    public synchronized float getDt() {
        return dt;
    }

    public boolean check(GameObject t) {
        return  (x <= t.getX() && t.getX() <= x + width) && (y <= t.getY() && t.getY() <= y + height) ||
                (x <= t.getX() && t.getX() <= x + width) && (y <= t.getY() + t.getHeight() && t.getY() + t.getHeight() <= y + height) ||
                (x <= t.getX() + t.getWidth() &&  t.getX() + t.getWidth() <= x + width) && (y <= t.getY() && t.getY() <= y + height) ||
                (x <= t.getX() + t.getWidth() &&  t.getX() + t.getWidth() <= x + width) && (y <= t.getY() + t.getHeight() && t.getY() + t.getHeight() <= y + height);
    }

    public synchronized void update() {
        cur = System.currentTimeMillis();
        dt = cur - last;
        last = cur;

        if(direction == 1) y += speed * dt / 1000.0;
        if(direction == 2) y -= speed * dt / 1000.0;
        if(direction == 3) x -= speed * dt / 1000.0;
        if(direction == 4) x += speed * dt / 1000.0;


        x = ((int)(x * 10)) /(float)(10);
        y = ((int)(y * 10)) /(float)(10);
    }
}

