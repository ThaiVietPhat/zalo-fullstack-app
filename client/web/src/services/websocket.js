import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client/dist/sockjs.js';

class WebSocketService {
  constructor() {
    this.client = null;
    this.subscriptions = {};
    this.connected = false;
    this.onConnectCallbacks = [];
  }

  connect(token, onConnect) {
    // Already connected — run callback immediately
    if (this.connected) {
      if (onConnect) onConnect();
      return;
    }

    // Queue the callback (works whether we're mid-connect or just starting)
    if (onConnect) this.onConnectCallbacks.push(onConnect);

    // Already connecting — don't create a second client
    if (this.client) return;

    this.client = new Client({
      webSocketFactory: () => new SockJS(`${window.location.protocol}//${window.location.hostname}:8080/ws`),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      onConnect: () => {
        this.connected = true;
        this.onConnectCallbacks.forEach((cb) => cb());
        this.onConnectCallbacks = [];
      },
      onDisconnect: () => {
        this.connected = false;
      },
      onStompError: (frame) => {
        console.error('STOMP error', frame);
      },
    });

    this.client.activate();
  }

  disconnect() {
    if (this.client) {
      Object.values(this.subscriptions).forEach((sub) => {
        try { sub.unsubscribe(); } catch {}
      });
      this.subscriptions = {};
      this.client.deactivate();
      this.client = null;
      this.connected = false;
    }
  }

  subscribe(destination, callback) {
    if (!this.client) return null;

    if (this.connected) {
      if (this.subscriptions[destination]) {
        try { this.subscriptions[destination].unsubscribe(); } catch {}
      }
      const sub = this.client.subscribe(destination, (message) => {
        try {
          const data = JSON.parse(message.body);
          callback(data);
        } catch (e) {
          callback(message.body);
        }
      });
      this.subscriptions[destination] = sub;
      return sub;
    } else {
      // Queue for after connect
      this.onConnectCallbacks.push(() => {
        this.subscribe(destination, callback);
      });
      return null;
    }
  }

  unsubscribe(destination) {
    if (this.subscriptions[destination]) {
      try { this.subscriptions[destination].unsubscribe(); } catch {}
      delete this.subscriptions[destination];
    }
  }

  publish(destination, body) {
    if (this.client && this.connected) {
      this.client.publish({
        destination,
        body: JSON.stringify(body),
      });
    }
  }

  isConnected() {
    return this.connected;
  }
}

const wsService = new WebSocketService();
export default wsService;
