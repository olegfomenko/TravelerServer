import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.xml.crypto.Data;


public class Main {
    static volatile HashMap<Integer, Tank> tanks;
    static volatile ArrayList<Wall> walls;
    static volatile ArrayList<Ball> balls;

    static volatile DatagramSocket socket;

    public static void main(String[] args) throws IOException {

        tanks = new HashMap<>();
        walls = new ArrayList<>();
        balls = new ArrayList<>();

        walls.add(new Wall(1000, 1000, 50, 1000));
        walls.add(new Wall(1000, 1000, 1000, 50));
        walls.add(new Wall(1000, 2000, 1000, 50));
        walls.add(new Wall(2000, 1000, 50, 1050));
    
        socket = new DatagramSocket(5000);
        socket.setBroadcast(true);

        Updater upd = new Updater();
        new Thread(upd).start();

        while(true) {
            byte[] buffer = new byte[1000000];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            new Thread(new Handler(packet)).start();
        }
    }

    static class Handler implements Runnable {
        DatagramPacket packet;

        public Handler(DatagramPacket packet) {
            this.packet = packet;
        }

        public void run() {
            try {
                byte[] buffer;
                buffer = packet.getData();

                int last = 0;
                for (; last < buffer.length; ++last) if (buffer[last] == 0) break;

                String s = new String(buffer, 0, last);
                System.out.println(packet.getAddress() + "  " + packet.getPort() + " " + s);
                JSONObject obj = new JSONObject(s);

                if (obj.getString("type").equals("CREATE")) {
                    //создаем танк
                    Tank tank = new Tank(packet.getAddress(), packet.getPort(), walls, balls);
                    tanks.put(tank.getId(), tank);


                    //говорим новому клиенту его id и инфу об остальных гроках
                    obj = new JSONObject();
                    obj.put("type", "CREATED");
                    obj.put("i", tank.getId());
                    obj.put("TANKS", getAllTanks());
                    obj.put("WALLS", getAllWalls());
                    obj.put("BALLS", getAllBalls());


                    send(obj.toString(), socket, tank.getAddress(), tank.getPort());
                } else {
                    int id = obj.getInt("i");

                    if (obj.getString("type").equals("GET")) {
                        JSONObject request = new JSONObject();
                        request.put("c", obj.getInt("c"));
                        request.put("type", "UPDATE");
                        request.put("TANKS", getAllTanks());
                        request.put("BALLS", getAllBalls());

                        send(request.toString(), socket, packet.getAddress(), packet.getPort());
                    }

                    if (obj.getString("type").equals("UPDATE")) {
                        int direction = obj.getInt("d");
                        tanks.get(id).setDirection(direction);
                    }

                }
            } catch(JSONException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void send(String request, DatagramSocket socket, InetAddress address, int port) throws IOException {
        byte[] buffer = request.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);

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

    static class Updater implements Runnable {

        @Override
        public void run() {
            while(true) {
                ArrayList<Tank> tl;
                synchronized (tanks) {
                    tl = new ArrayList<>(tanks.values());
                }
                for(int i = 0; i < tl.size(); ++i) tl.get(i).update();

                for(int i = 0; i < balls.size(); ++i) balls.get(i).update();

                for(int i = 0; i < tl.size(); ++i)
                    for(int j = 0; j < balls.size(); ++j)
                        if(tl.get(i).check(balls.get(j))) synchronized (tanks) {
                            System.out.println(tl.get(i).getId() + " has been removed!");
                            tanks.remove(tl.get(i).getId());
                            break;
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






