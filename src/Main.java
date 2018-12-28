    import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
    import org.omg.CORBA.PUBLIC_MEMBER;

    public class Main {
    static volatile HashMap<Integer, Tank> tanks;
    static volatile ArrayList<Wall> walls;
    static volatile ArrayList<Ball> balls;

    public static void main(String[] args) throws IOException {

        tanks = new HashMap<>();
        walls = new ArrayList<>();
        balls = new ArrayList<>();

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
                    Tank tank = new Tank(packet.getAddress(), walls, balls);
                    tanks.put(tank.getId(), tank);


                    //говорим новому клиенту его id и инфу об остальных гроках
                    obj = new JSONObject();
                    obj.put("type", "CREATED");
                    obj.put("i", tank.getId());
                    obj.put("TANKS", getAllTanks());
                    obj.put("WALLS", getAllWalls());
                    obj.put("BALLS", getAllBalls());

                    send(obj.toString(), socket, tank.getAddress());
                } else {
                    int id = obj.getInt("i");

                    if (obj.getString("type").equals("GET")) {
                        JSONObject request = new JSONObject();
                        request.put("c", obj.getInt("c"));
                        request.put("type", "UPDATE");
                        request.put("TANKS", getAllTanks());
                        request.put("BALLS", getAllBalls());

                        send(request.toString(), socket, packet.getAddress());
                    }

                    if (obj.getString("type").equals("UPDATE")) {
                        int direction = obj.getInt("d");
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
        synchronized (tanks) {
            for(Tank tank : tanks.values()) {
                JSONObject t = new JSONObject();
                t.put("i", tank.getId());
                t.put("x", tank.getX());
                t.put("y", tank.getY());
                t.put("d", tank.getDirection());
                arr.put(t);
            }
        }

        return arr;
    }

    private static JSONArray getAllWalls() throws JSONException {
        JSONArray arr = new JSONArray();

        synchronized (walls) {
            for (Wall w : walls) {
                JSONObject wo = new JSONObject();
                wo.put("x", w.getX());
                wo.put("y", w.getY());
                wo.put("w", w.getWidth());
                wo.put("h", w.getHeight());
                arr.put(wo);
            }
        }

        return arr;
    }

    private static JSONArray getAllBalls() throws JSONException {
        JSONArray arr = new JSONArray();

        synchronized (balls) {
            for(Ball b : balls) {
                JSONObject bo = new JSONObject();
                bo.put("i", b.getId());
                bo.put("x", b.getX());
                bo.put("y", b.getY());
                bo.put("d", b.getDirection());
                arr.put(bo);
            }
        }

        return arr;
    }

    /*static class Send implements Runnable {
        private JSONObject request;
        private DatagramSocket socket;
        private InetAddress address;

        public Send(JSONObject request, DatagramSocket socket, InetAddress address) {
            this.request = request;
            this.socket = socket;
            this.address = address;
        }

        @Override
        public void run() {
            try {
                byte[] buffer = request.toString().getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 5000);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }*/

    static class Updater implements Runnable {

        @Override
        public void run() {
            while(true) {
                Object[] o = tanks.values().toArray();
                for(int i = 0; i < o.length; ++i) {
                    Tank t = (Tank)o[i];
                    t.update();
                }

                for(int i = 0; i < balls.size(); ++i) balls.get(i).update();

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}






