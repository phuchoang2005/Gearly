import api from '../api';

export const updateUser = (data) => {
    return api.post('/users/update', data);
};

export const deactivateUserAccount = () => {
    return api.post('/users/deactivate');
}

export const uploadAvatar = (formData) => {
    return api.post("/users/upload-avatar", formData, {
        headers: {
            "Content-Type": "multipart/form-data",
        },
    });
};

export const changePassword = (data) => {
    return api.post("/users/change-password", data);
}
