import * as yup from 'yup';

export const registerSchema = yup.object().shape({
    firstName: yup.string().required('First name is required'),
    lastName: yup.string().required('Last name is required'),
    email: yup.string().email('Invalid email').required('Email is required'),
    phoneNumber: yup
        .string()
        .required('Phone number is required')
        .matches(/^\+?[1-9]\d{7,14}$/, 'Invalid phone number format'),
    password: yup
        .string()
        .required('Password is required')
        .min(8, 'Password must be at least 8 characters')
        .matches(/[A-Z]/, 'Must contain at least one uppercase letter')
        .matches(/[a-z]/, 'Must contain at least one lowercase letter')
        .matches(/[0-9]/, 'Must contain at least one number')
        .matches(/[^A-Za-z0-9]/, 'Must contain at least one special character'),
    confirmPassword: yup
        .string()
        .oneOf([yup.ref('password'), null], 'Passwords must match')
        .required('Confirm password is required'),
    agreeToTerms: yup
        .boolean()
        .oneOf([true], 'You must agree to the Terms and Conditions'),

    streetAddress: yup
        .string()
        .trim()
        .max(255, 'Street address must be 255 characters or fewer')
        .notRequired(),

    city: yup
        .string()
        .trim()
        .notRequired()
        .matches(/^\d+$/, 'Invalid city selection'),

    state: yup
        .string()
        .trim()
        .notRequired()
        .matches(/^\d+$/, 'Invalid state selection'),

    postalCode: yup
        .string()
        .trim()
        .notRequired()
        .test(
            'valid-postal',
            'Invalid postal code format',
            (value) => !value || /^[A-Za-z0-9\s\-]{3,10}$/.test(value)
        ),

    country: yup
        .string()
        .trim()
        .notRequired()
        .matches(/^\d+$/, 'Invalid country selection'),
});

export const loginSchema = yup.object({
    email: yup
        .string()
        .email('Invalid email format')
        .required('Email is required'),
    password: yup
        .string()
        .required('Password is required'),
    rememberMe: yup.boolean()
});

export const forgotPasswordSchema = yup.object({
    email: yup
        .string()
        .email('Invalid email format')
        .required('Email is required'),
});

export const resendVerificationSchema = yup.object({
    email: yup.string().email('Invalid email format').required('Email is required'),
});

export const resetPasswordSchema = yup.object({
    newPassword: yup
        .string()
        .required('New password is required')
        .min(8, 'Must be at least 8 characters')
        .matches(/[A-Z]/, 'Must include an uppercase letter')
        .matches(/[a-z]/, 'Must include a lowercase letter')
        .matches(/[0-9]/, 'Must include a number')
        .matches(/[^A-Za-z0-9]/, 'Must include a special character'),

    confirmPassword: yup
        .string()
        .oneOf([yup.ref('newPassword'), null], 'Passwords must match')
        .required('Please confirm your password'),
});

export const checkoutSchema = yup.object().shape({
    firstName: yup.string().required("First name is required"),
    lastName: yup.string().required("Last name is required"),
    phoneNumber: yup
        .string()
        .required("Phone number is required")
        .matches(/^\+?[1-9]\d{7,14}$/, "Invalid phone number format"),
    email: yup.string().email("Invalid email").required("Email is required"),
    country: yup.string().required("Country is required"),
    state: yup.string().required("State/Province is required"),
    city: yup.string().required("City/Town is required"),
    postalCode: yup.string().required("Postal/Zip Code is required"),
    street: yup.string().required("Street Address is required"),
});


export const profileSchema = yup.object({
    firstName: yup.string().required("First name is required"),
    lastName: yup.string().required("Last name is required"),
    email: yup.string().email("Invalid email").required("Email is required"),
    phone: yup
        .string()
        .required("Phone number is required")
        .matches(/^\+?[1-9]\d{7,14}$/, "Invalid phone number format"),
})

export const addressSchema = yup
    .object({
        country: yup
            .string()
            .trim()
            .notRequired()
            .matches(/^\d+$/, "Invalid country"),

        state: yup
            .string()
            .trim()
            .notRequired()
            .matches(/^\d+$/, "Invalid state"),

        city: yup
            .string()
            .trim()
            .notRequired()
            .matches(/^\d+$/, "Invalid city"),

        postalCode: yup
            .string()
            .trim()
            .notRequired()
            .test(
                "valid-postal",
                "Invalid postal code format",
                (value) => {
                    return !value || /^[A-Za-z0-9\s\-]{3,10}$/.test(value)
                }
            ),

        street: yup
            .string()
            .trim()
            .notRequired()
            .max(255, "Street address must be 255 characters or fewer"),
    })

    .test(
        "address-complete",
        "All address fields are required if any address field is filled",
        (value) => {
            const {country, state, city, postalCode, street} = value
            const anyFilled =
                !!country || !!state || !!city || !!postalCode || !!street
            if (!anyFilled) return true
            return (
                !!country &&
                !!state &&
                !!city &&
                !!postalCode &&
                !!street
            )
        }
    )

export const changePasswordSchema = yup.object({
    currentPassword: yup
        .string()
        .required("Current password is required"),

    newPassword: yup
        .string()
        .required("New password is required")
        .min(8, "Must be at least 8 characters")
        .matches(/[A-Z]/, "Must include an uppercase letter")
        .matches(/[a-z]/, "Must include a lowercase letter")
        .matches(/[0-9]/, "Must include a number")
        .matches(/[^A-Za-z0-9]/, "Must include a special character"),

    confirmPassword: yup
        .string()
        .oneOf([yup.ref("newPassword"), null], "Passwords must match")
        .required("Please confirm your password"),
});

