import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Main {

    JSONObject o = new JSONObject();


    static int index = 0;
    static HashMap<Integer, Tank> tanks;

    public static void main(String[] args) throws IOException, JSONException {

        tanks = new HashMap<>();
        DatagramSocket socket = new DatagramSocket(5000);

        Updater upd = new Updater();
        new Thread(upd).start();

        while(true) {
            byte[] buffer = new byte[1000000];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            socket.receive(packet);
            buffer = packet.getData();

            int last = 0;
            for(; last < buffer.length; ++last) if(buffer[last] == 0) break;

            String s = new String(buffer, 0, last);
            System.out.println(packet.getAddress() + "  " + s);
            JSONObject obj = new JSONObject(s);

            if(obj.getString("type").equals("CREATE")) {
                //создаем танк
                Tank tank = new Tank(packet.getAddress(), index++);

                // информируем всех игроков о подключении нового игрока
                for(Tank t : tanks.values()) {
                    JSONObject request = new JSONObject();
                    request.put("type", "ADD");
                    request.put("index", tank.id);
                    request.put("x", tank.getX());
                    request.put("y", tank.getY());
                    request.put("direction", tank.getDirection());
                    send(request.toString(), socket, t.address);
                }

                //добавляем в список нового клиента
                tanks.put(tank.id, tank);


                //говорим новому клиенту его id и инфу об остальных гроках
                obj = new JSONObject();
                obj.put("index", tank.id);
                obj.put("TANKS", getAllTanks());
                send(obj.toString(), socket, tank.address);
            } else {
                int id = obj.getInt("index");

                if(obj.getString("type").equals("GET")) {
                    JSONObject request = new JSONObject();
                    request.put("check_code", obj.getInt("check_code"));
                    request.put("type", "UPDATE");
                    request.put("TANKS", getAllTanks());

                    send(request.toString(), socket, packet.getAddress());
                }

                if(obj.getString("type").equals("UPDATE")) {
                    int direction = obj.getInt("direction");
                    tanks.get(id).setDirection(direction);
                }

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


class Tank {

    InetAddress address;
    int id;
    private volatile float x = 1500, y = 1500;
    private volatile int direction = 1; // если один поток измемняет это на объекте, то оно изменяется и на другом потоке
    long last = System.currentTimeMillis(), cur, dt;
    long speed = 120;

    public Tank(InetAddress address, int id) {
        this.address = address;
        this.id = id;
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
            cur = System.currentTimeMillis();
            dt = cur - last;

            last = cur;

            //System.out.println(direction);

            if(direction == 1) y += speed * dt / 1000.0;
            if(direction == 2) y -= speed * dt / 1000.0;
            if(direction == 3) x -= speed * dt / 1000.0;
            if(direction == 4) x += speed * dt / 1000.0;

            x = ((int)(x * 1000)) /(float)(1000);
            y = ((int)(y * 1000)) /(float)(1000);
        }
    }

	/*@Override
	public void run() {
		synchronized(this) {
			while(true) {
				//System.out.println(direction);
				cur = System.currentTimeMillis();
				dt = cur - last;

				last = cur;

				//System.out.println(direction);

				if(direction == 1) y += speed * dt / 1000.0;
				if(direction == 2) y -= speed * dt / 1000.0;
				if(direction == 3) x -= speed * dt / 1000.0;
				if(direction == 4) x += speed * dt / 1000.0;
			}
		}
	}*/

}