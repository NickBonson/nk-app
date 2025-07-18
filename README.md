## Created with Capacitor Create App

This app was created using [`@capacitor/create-app`](https://github.com/ionic-team/create-capacitor-app),
and comes with a very minimal shell for building an app.

### Running this example

To run the provided example, you can use `npm start` command.

```bash
npm start
```

### Passkey Plugin

The project includes a Capacitor plugin that uses Android's Credential Manager API to handle passkey based registration and login.

#### JavaScript usage

```javascript
import { registerWithPasskey, loginWithPasskey } from './js/passkey.js';

const registerResult = await registerWithPasskey({
  challenge: '<challenge-from-backend>',
  userId: '<user-id>',
  userName: '<user-name>',
  rpId: '<rp-id>',
  rpName: '<rp-name>'
});

const loginResult = await loginWithPasskey({
  challenge: '<challenge-from-backend>',
  rpId: '<rp-id>'
});
```

The plugin requires Android 9 or later and returns credential data back to JavaScript.
