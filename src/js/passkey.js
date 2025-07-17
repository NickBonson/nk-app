import { registerPlugin } from '@capacitor/core';

const Passkey = registerPlugin('Passkey');

export const registerWithPasskey = (options) => {
  return Passkey.registerWithPasskey(options);
};

export const loginWithPasskey = (options) => {
  return Passkey.loginWithPasskey(options);
};

export default {
  registerWithPasskey,
  loginWithPasskey,
};
