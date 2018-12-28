public class Wall {
    private float x, y, width, height;

    public Wall(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public boolean check(GameObject t) {
        return  (x <= t.getX() && t.getX() <= x + width) && (y <= t.getY() && t.getY() <= y + height) ||
                (x <= t.getX() && t.getX() <= x + width) && (y <= t.getY() + t.getHeight() && t.getY() + t.getHeight() <= y + height) ||
                (x <= t.getX() + t.getWidth() &&  t.getX() + t.getWidth() <= x + width) && (y <= t.getY() && t.getY() <= y + height) ||
                (x <= t.getX() + t.getWidth() &&  t.getX() + t.getWidth() <= x + width) && (y <= t.getY() + t.getHeight() && t.getY() + t.getHeight() <= y + height);
    }
}