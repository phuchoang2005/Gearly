import {useEffect, useMemo} from "react";
import {useForm, Controller} from "react-hook-form";
import {yupResolver} from "@hookform/resolvers/yup";
import {registerSchema} from "@utils/validate.js";
import {useRegister} from "@u_hooks/useRegister.js";
import FormInput from "../shared/FormInput.jsx";
import {Link} from "react-router-dom";
import GoogleLoginButton from "./GoogleLoginButton.jsx";
import PhoneInput from "react-phone-input-2";
import "react-phone-input-2/lib/style.css";
import {useAddressData} from "@u_hooks/useAddressData.js";

export default function RegisterForm() {
    const {loading, submit} = useRegister();

    const {
        register,
        handleSubmit,
        control,
        watch,
        setValue,
        formState: {errors},
    } = useForm({
        resolver: yupResolver(registerSchema),
    });

    const selectedCountry = watch("country");
    const selectedState = watch("state");

    // reset dependent fields
    useEffect(() => {
        setValue("state", "");
        setValue("city", "");
    }, [selectedCountry, setValue]);

    useEffect(() => {
        setValue("city", "");
    }, [selectedState, setValue]);

    const {
        countries,
        isLoadingCountries,
        isErrorCountries,

        statesList,
        isLoadingStates,
        isErrorStates,

        citiesList,
        isLoadingCities,
        isErrorCities,
    } = useAddressData(selectedCountry, selectedState);

    const countryMap = useMemo(() => new Map(countries.map(c => [c.value, c.label])), [countries]);
    const stateMap = useMemo(() => new Map(statesList.map(s => [s.value, s.label])), [statesList]);
    const cityMap = useMemo(() => new Map(citiesList.map(c => [c.value, c.label])), [citiesList]);

    const onFormSubmit = (data) => {
        const countryId = Number.isInteger(+data.country) ? +data.country : null;
        const stateId = Number.isInteger(+data.state) ? +data.state : null;
        const cityId = Number.isInteger(+data.city) ? +data.city : null;

        const countryLabel = countryMap.get(countryId) || "";
        const stateLabel = stateMap.get(stateId) || "";
        const cityLabel = cityMap.get(cityId) || "";

        submit({
            ...data,
            country: countryLabel,
            state: stateLabel,
            city: cityLabel,
        });
    };

    return (
        <form onSubmit={handleSubmit(onFormSubmit)} className="space-y-6">
            <div className="grid gap-6 md:grid-cols-2">
                <FormInput
                    label="First Name"
                    required
                    placeholder="Enter First Name"
                    {...register("firstName")}
                    error={errors.firstName?.message}
                    className="bg-white"
                />
                <FormInput
                    label="Last Name"
                    required
                    placeholder="Enter Last Name"
                    {...register("lastName")}
                    error={errors.lastName?.message}
                    className="bg-white"
                />
            </div>

            <FormInput
                label="Email"
                required
                placeholder="Enter Email Address"
                {...register("email")}
                error={errors.email?.message}
                autoComplete="email"
                className="bg-white"
            />

            <div className="w-full">
                <label
                    htmlFor="phoneNumber"
                    className="block text-sm font-medium text-gray-900 mb-1"
                >
                    Phone Number <span className="text-red-600">*</span>
                </label>
                <Controller
                    name="phoneNumber"
                    control={control}
                    render={({field}) => (
                        <PhoneInput
                            {...field}
                            country="vn"
                            inputProps={{
                                name: "phoneNumber",
                                required: true,
                                autoComplete: "tel",
                                id: "phoneNumber",
                            }}
                            inputClass={`w-full text-sm px-3 py-2 border rounded bg-white focus:outline-none 
                focus:ring-1 focus:ring-black focus:border-black
                ${errors.phoneNumber ? "border-red-500 focus:ring-red-500" : "border-gray-300"}`}
                            inputStyle={{width: "100%", color: "#111827"}}
                        />
                    )}
                />
                {errors.phoneNumber && (
                    <p className="text-sm text-red-500 mt-1">
                        {errors.phoneNumber.message}
                    </p>
                )}
            </div>

            <div className="grid gap-6 md:grid-cols-2">
                {/* Country */}
                <div>
                    <label
                        htmlFor="country"
                        className="block text-sm font-medium text-gray-900 mb-1"
                    >
                        Country
                    </label>
                    <Controller
                        name="country"
                        control={control}
                        render={({field}) => (
                            <select
                                {...field}
                                id="country"
                                disabled={isLoadingCountries || isErrorCountries}
                                className={`w-full text-sm px-3 py-2 border rounded focus:outline-none focus:ring-1 focus:ring-black focus:border-black cursor-pointer disabled:cursor-not-allowed
                                  ${errors.country ? "border-red-500 focus:ring-red-500" : "border-gray-300"} 
                                  ${isLoadingCountries || isErrorCountries ? "bg-gray-100" : "bg-white"}`}
                            >
                                {isLoadingCountries ? (
                                    <option>Loading countries…</option>
                                ) : isErrorCountries ? (
                                    <option>Error loading countries</option>
                                ) : (
                                    <>
                                        <option value="">Select Country</option>
                                        {countries.map((c) => (
                                            <option key={c.value} value={c.value}>
                                                {c.label}
                                            </option>
                                        ))}
                                    </>
                                )}
                            </select>
                        )}
                    />
                    {errors.country && (
                        <p className="text-sm text-red-500 mt-1">
                            {errors.country.message}
                        </p>
                    )}
                </div>

                {/* State */}
                <div>
                    <label
                        htmlFor="state"
                        className="block text-sm font-medium text-gray-900 mb-1"
                    >
                        State / Province
                    </label>
                    <Controller
                        name="state"
                        control={control}
                        render={({field}) => (
                            <select
                                {...field}
                                id="state"
                                disabled={!selectedCountry || isLoadingStates || isErrorStates}
                                className={`w-full text-sm px-3 py-2 border rounded focus:outline-none focus:ring-1 focus:ring-black focus:border-black cursor-pointer disabled:cursor-not-allowed
                                  ${errors.state ? "border-red-500 focus:ring-red-500" : "border-gray-300"} 
                                  ${!selectedCountry || isLoadingStates || isErrorStates ? "bg-gray-100" : "bg-white"}`}
                            >
                                {isLoadingStates ? (
                                    <option>Loading states…</option>
                                ) : isErrorStates ? (
                                    <option>Error loading states</option>
                                ) : (
                                    <>
                                        <option value="">Select State</option>
                                        {statesList.map((s) => (
                                            <option key={s.value} value={s.value}>
                                                {s.label}
                                            </option>
                                        ))}
                                    </>
                                )}
                            </select>
                        )}
                    />
                    {errors.state && (
                        <p className="text-sm text-red-500 mt-1">
                            {errors.state.message}
                        </p>
                    )}
                </div>
            </div>

            <div className="grid gap-6 md:grid-cols-2">
                {/* City */}
                <div>
                    <label
                        htmlFor="city"
                        className="block text-sm font-medium text-gray-900 mb-1"
                    >
                        City / District
                    </label>
                    <Controller
                        name="city"
                        control={control}
                        render={({field}) => (
                            <select
                                {...field}
                                id="city"
                                disabled={!selectedState || isLoadingCities || isErrorCities}
                                className={`w-full text-sm px-3 py-2 border rounded focus:outline-none focus:ring-1 focus:ring-black focus:border-black cursor-pointer disabled:cursor-not-allowed
                                  ${errors.city ? "border-red-500 focus:ring-red-500" : "border-gray-300"} 
                                  ${!selectedState || isLoadingCities || isErrorCities ? "bg-gray-100" : "bg-white"}`}
                            >
                                {isLoadingCities ? (
                                    <option>Loading cities…</option>
                                ) : isErrorCities ? (
                                    <option>Error loading cities</option>
                                ) : (
                                    <>
                                        <option value="">Select City</option>
                                        {citiesList.map((c) => (
                                            <option key={c.value} value={c.value}>
                                                {c.label}
                                            </option>
                                        ))}
                                    </>
                                )}
                            </select>
                        )}
                    />
                    {errors.city && (
                        <p className="text-sm text-red-500 mt-1">
                            {errors.city.message}
                        </p>
                    )}
                </div>

                <FormInput
                    label="Postal / Zip Code"
                    placeholder="Enter Postal Code"
                    {...register("postalCode")}
                    error={errors.postalCode?.message}
                    className="bg-white"
                />
            </div>

            <FormInput
                label="Street Address"
                placeholder="Enter Street Address"
                {...register("streetAddress")}
                error={errors.streetAddress?.message}
                className="bg-white"
            />

            <FormInput
                label="Password"
                type="password"
                required
                placeholder="Enter Password"
                {...register("password")}
                error={errors.password?.message}
                autoComplete="new-password"
                className="bg-white"
            />

            <FormInput
                label="Re-enter Password"
                type="password"
                required
                placeholder="Re-enter Password"
                {...register("confirmPassword")}
                error={errors.confirmPassword?.message}
                autoComplete="new-password"
                className="bg-white"
            />

            <div className="flex items-start gap-2">
                <input
                    type="checkbox"
                    id="agreeToTerms"
                    {...register("agreeToTerms")}
                    className="mt-0.5 accent-black w-4 h-4 cursor-pointer"
                />
                <label htmlFor="agreeToTerms" className="text-xs text-gray-700">
                    I confirm that I have read and agree to the&nbsp;
                    <a href="/terms" className="text-black underline hover:text-gray-700">
                        Terms of Service
                    </a>
                    &nbsp;and&nbsp;
                    <a href="/privacy" className="text-black underline hover:text-gray-700">
                        Privacy Policy
                    </a>
                    .
                </label>
            </div>
            {errors.agreeToTerms && (
                <p className="text-xs text-red-500 mt-1">{errors.agreeToTerms.message}</p>
            )}

            <button
                type="submit"
                disabled={loading}
                className="w-1/4 mx-auto block bg-black text-white text-sm font-semibold py-3 px-4 rounded-md mt-10
                  shadow-[0_4px_14px_0_rgba(0,0,0,0.25)]
                  hover:shadow-[0_6px_20px_rgba(0,0,0,0.35)]
                  transition-all duration-200 ease-in-out
                  disabled:opacity-50 disabled:cursor-not-allowed!"
            >
                {loading ? "Please wait…" : "Sign Up"}
            </button>

            <GoogleLoginButton/>

            <p className="text-center text-sm">
                Already have an account?{" "}
                <Link to="/login" className="text-black underline hover:text-gray-700">
                    Log in
                </Link>
            </p>
        </form>
    );
}
