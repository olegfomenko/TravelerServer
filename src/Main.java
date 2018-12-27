    import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Main {

    JSONObject o = new JSONObject();


    static int index = 0;
    static HashMap<Integer, Tank> tanks;
    static ArrayList<Wall> walls;

    public static void main(String[] args) throws IOException {

        tanks = new HashMap<>();
        walls = new ArrayList<>();

        walls.add(new Wall(1000, 1000, 50, 1000));
        walls.add(new Wall(1000, 1000, 1000, 50));
        walls.add(new Wall(1000, 2000, 1000, 50));
        walls.add(new Wall(2000, 1000, 50, 1050));

        DatagramSocket socket = new DatagramSocket(5000);

        Updater upd = new Updater();
        new Thread(upd).start();

        while(true) {
            try {
                byte[] buffer = new byte[1000000];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                socket.receive(packet);
                buffer = packet.getData();

                int last = 0;
                for (; last < buffer.length; ++last) if (buffer[last] == 0) break;

                String s = new String(buffer, 0, last);
                System.out.println(packet.getAddress() + "  " + s);
                JSONObject obj = new JSONObject(s);

                if (obj.getString("type").equals("CREATE")) {
                    //создаем танк
                    Tank tank = new Tank(packet.getAddress(), index++, walls);

                    /*// информируем всех игроков о подключении нового игрока
                    for (Tank t : tanks.values()) {
                        JSONObject request = new JSONObject();
                        request.put("type", "ADD");
                        request.put("index", tank.id);
                        request.put("x", tank.getX());
                        request.put("y", tank.getY());
                        request.put("direction", tank.getDirection());
                        send(request.toString(), socket, t.address);
                    }*/

                    //добавляем в список нового клиента
                    tanks.put(tank.id, tank);


                    //говорим новому клиенту его id и инфу об остальных гроках
                    obj = new JSONObject();
                    obj.put("type", "CREATED");
                    obj.put("index", tank.id);
                    obj.put("TANKS", getAllTanks());
                    obj.put("WALLS", getAllWalls());
                    send(obj.toString(), socket, tank.address);
                } else {
                    int id = obj.getInt("index");

                    if (obj.getString("type").equals("GET")) {
                        JSONObject request = new JSONObject();
                        request.put("check_code", obj.getInt("check_code"));
                        request.put("type", "UPDATE");
                        request.put("TANKS", getAllTanks());

                        send(request.toString(), socket, packet.getAddress());
                    }

                    if (obj.getString("type").equals("UPDATE")) {
                        int direction = obj.getInt("direction");
                        tanks.get(id).setDirection(direction);
                    }

                }
            } catch(JSONException e) {
                e.printStackTrace();
            }
        }

    }

    private static void send(String request, DatagramSocket socket, InetAddress address) throws IOException {
        byte[] buffer = request.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 5000);
        socket.send(packet);
    }

    private static JSONArray getAllTanks() throws JSONException {
        JSONArray arr = new JSONArray();
        for(Tank tank : tanks.values()) {
            JSONObject t = new JSONObject();
            t.put("index", tank.id);
            t.put("x", tank.getX());
            t.put("y", tank.getY());
            t.put("direction", tank.getDirection());
            arr.put(t);
        }
        return arr;
    }

    private static JSONArray getAllWalls() throws JSONException {
        JSONArray arr = new JSONArray();
        for(Wall w : walls) {
            JSONObject wo = new JSONObject();
            wo.put("x", w.getX());
            wo.put("y", w.getY());
            wo.put("width", w.getWidth());
            wo.put("height", w.getHeight());
            arr.put(wo);
        }

        return arr;
    }

    static class Updater implements Runnable {

        @Override
        public void run() {
            while(true) {
                Object[] o = tanks.values().toArray();
                for(int i = 0; i < o.length; ++i) {
                    Tank t = (Tank)o[i];
                    t.update();
                }

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

}

class Wall {
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

    public boolean check(Tank t) {
        return  (x <= t.getX() && t.getX() <= x + width) && (y <= t.getY() && t.getY() <= y + height) ||
                (x <= t.getX() && t.getX() <= x + width) && (y <= t.getY() + Tank.height && t.getY() + Tank.height <= y + height) ||
                (x <= t.getX() + Tank.width &&  t.getX() + Tank.width <= x + width) && (y <= t.getY() && t.getY() <= y + height) ||
                (x <= t.getX() + Tank.width &&  t.getX() + Tank.width <= x + width) && (y <= t.getY() + Tank.height && t.getY() + Tank.height <= y + height);
    }
}


class Tank {
    InetAddress address;
    int id;
    private volatile float x = 1500, y = 1500;
    public static final float width = 64, height = 64;
    private volatile int direction = 1; // если один поток измемняет это на объекте, то оно изменяется и на другом потоке
    long last = System.currentTimeMillis(), cur, dt;
    long speed = 120;
    ArrayList<Wall> walls;

    public Tank(InetAddress address, int id, ArrayList<Wall> walls) {
        this.address = address;
        this.id = id;
        this.walls = walls;

        x = (float)(1100 + Math.random() * 800);
        y = (float)(1100 + Math.random() * 800);

        direction = (int)(1 + Math.random() * 4);
    }

    public synchronized void setDirection(int direction) {
        this.direction = direction;
    }

    public synchronized float getX() {
        return x;
    }

    public synchronized float getY() {
        return y;
    }

    public synchronized float getDirection() {
        return direction;
    }

    public synchronized void update() {
        synchronized(this) {
            for (Wall w : walls) if(w.check(this)) {
                switch (direction) {
                    case 1: direction = 2; break;
                    case 2: direction = 1; break;
                    case 3: direction = 4; break;
                    case 4: direction = 3; break;
                }
                break;
            }


            cur = System.currentTimeMillis();
            dt = cur - last;

            last = cur;

            if(direction == 1) y += speed * dt / 1000.0;
            if(direction == 2) y -= speed * dt / 1000.0;
            if(direction == 3) x -= speed * dt / 1000.0;
            if(direction == 4) x += speed * dt / 1000.0;


            x = ((int)(x * 1000)) /(float)(1000);
            y = ((int)(y * 1000)) /(float)(1000);
        }
    }
}
