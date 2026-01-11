import api from '../api';

export const registerUser = (data) => {
    return api.post('/users/register', data);
};

export const loginUser = (data) => {
    return api.post('/users/login', data);
};

export const logoutUser = (date) => {
    return api.post('/users/logout');
}

export const resendVerificationEmail = (email) => {
    return api.post('/users/resend-verification', { email });
};

export const sendPasswordResetLink = (email) => {
    return api.post('/users/forgot-password', { email });
};

export const resetPassword = (data) => {
    return api.post('/users/reset-password', data);
};
