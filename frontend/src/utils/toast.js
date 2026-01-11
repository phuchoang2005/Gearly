import toast from 'react-hot-toast';

export const showSuccess = (msg) => toast.success(msg);
export const showError = (msg) => toast.error(msg);
export const showWarning = (msg) => toast.custom(msg);
export const showInfo = (msg) => toast(msg);

export const showPromise = async (promiseOrFn, messages = {}, options = {}) => {
    const promise = Promise.resolve(
        typeof promiseOrFn === 'function' ? promiseOrFn() : promiseOrFn
    );

    try {
        return await toast.promise(
            promise,
            {
                loading: messages.loading || 'Processing...',
                success: messages.success || 'Operation successful!',
                error: (err) => {
                    if (typeof messages.error === 'function') return messages.error(err);
                    return (
                        err?.response?.data?.error ||
                        messages.error ||
                        'Something went wrong.'
                    );
                },
            },
            {
                ...(options.toastOptions || {}),
            }
        );
    } catch (err) {
        if (options.throwOnError) throw err;
        return null;
    }
};
