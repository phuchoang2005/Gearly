// Validate numbers
export const safeNumber = (value, min, max, defaultValue) => {
    const num = parseFloat(value);
    return Number.isNaN(num) ? defaultValue : Math.max(min, Math.min(max, num));
};

// Validate integers
export const safeInteger = (value, min, max, defaultValue) => {
    const num = parseInt(value, 10);
    return Number.isNaN(num) ? defaultValue : Math.max(min, Math.min(max, num));
};

// Validate string
export const safeString = (value, validOptions, defaultValue) => {
    return validOptions.has(value) ? value : defaultValue;
};
