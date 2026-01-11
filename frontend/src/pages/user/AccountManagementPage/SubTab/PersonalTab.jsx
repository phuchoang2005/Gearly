import {useContext, useState, useEffect, useRef, useMemo} from "react"
import {useForm, Controller} from "react-hook-form"
import {yupResolver} from "@hookform/resolvers/yup"
import PhoneInput from "react-phone-input-2"
import {useAddressData} from "@u_hooks/useAddressData.js"
import {showPromise} from "@utils/toast.js"
import {User, Camera, Save, MapPin, AlertTriangle} from "lucide-react"
import FormInput from "@u_components/shared/FormInput.jsx"
import {AuthContext} from "@contexts/AuthContext.jsx"
import {profileSchema, addressSchema} from "/src/utils/validate.js"
import {deactivateUserAccount, updateUser, uploadAvatar} from "@u_services/userService.js"
import ConfirmationModal from "@u_components/modal/ConfirmationModal.jsx"
import {useNavigate} from "react-router-dom"
import {UserAvatar} from "@u_components/profile/UserAvatar.jsx"

export default function PersonalTab() {
const navigate = useNavigate()
const {auth, setAuth, logout} = useContext(AuthContext)
const user = auth?.user || {}

const [loading, setLoading] = useState(false)
const [profileImage, setProfileImage] = useState(null)
const [showSave, setShowSave] = useState(false)
const [showDeactivateModal, setShowDeactivateModal] = useState(false)

const formSectionRef = useRef(null)
const hasInitializedRef = useRef(false)
const prevCountryRef = useRef(null);
const prevStateRef = useRef(null);

const profileForm = useForm({
    resolver: yupResolver(profileSchema),
    defaultValues: {
        firstName: user.firstName || "",
        lastName: user.lastName || "",
        email: user.email || "",
        phone: user.phone || "",
    },
})

const addressForm = useForm({
    resolver: yupResolver(addressSchema),
    defaultValues: {
        country: user?.address?.countryId || "",
        state: user?.address?.stateId || "",
        city: user?.address?.cityId || "",
        postalCode: user?.address?.postalCode || "",
        street: user?.address?.street || "",
    },
})

const selectedCountry = addressForm.watch("country")
const selectedState = addressForm.watch("state")

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
} = useAddressData(selectedCountry, selectedState)

const countryIdMap = useMemo(
    () => new Map(countries.map(c => [c.value, c.label])),
    [countries]
)
const stateIdMap = useMemo(
    () => new Map(statesList.map(s => [s.value, s.label])),
    [statesList]
)
const cityIdMap = useMemo(
    () => new Map(citiesList.map(c => [c.value, c.label])),
    [citiesList]
)

useEffect(() => {
    if (hasInitializedRef.current) return;

    if (
        !user ||
        isLoadingCountries || isLoadingStates || isLoadingCities
    ) {
        return;
    }
    hasInitializedRef.current = true;

    profileForm.reset({
        firstName: user.firstName || "",
        lastName: user.lastName || "",
        email: user.email || "",
        phone: user.phone || "",
    })

    addressForm.reset({
        country: user?.address?.countryId || "",
        state: user?.address?.stateId || "",
        city: user?.address?.cityId || "",
        postalCode: user.address?.postalCode || "",
        street: user.address?.street || "",
    })

    if (user.profileAvatar) {
        setProfileImage(user.profileAvatar)
    }
}, [
    user,
    countries,
    statesList,
    citiesList
])

useEffect(() => {
    if (prevCountryRef.current && selectedCountry !== prevCountryRef.current) {
        addressForm.setValue("state", "");
        addressForm.setValue("city", "");
    }
    prevCountryRef.current = selectedCountry;
}, [selectedCountry]);

useEffect(() => {
    if (prevStateRef.current && selectedState !== prevStateRef.current) {
        addressForm.setValue("city", "");
    }
    prevStateRef.current = selectedState;
}, [selectedState]);

const eitherDirty =
    profileForm.formState.isDirty || addressForm.formState.isDirty

useEffect(() => {
    if (!eitherDirty) {
        setShowSave(false)
        return
    }
    const handleScroll = () => {
        if (!formSectionRef.current) {
            setShowSave(false)
            return
        }
        const topOfSection = formSectionRef.current.offsetTop
        if (window.scrollY + window.innerHeight >= topOfSection + 50) {
            setShowSave(true)
        } else {
            setShowSave(false)
        }
    }
    window.addEventListener("scroll", handleScroll, {passive: true})
    handleScroll()
    return () => window.removeEventListener("scroll", handleScroll)
}, [eitherDirty])

const handleSaveAll = async () => {
    try {
        const profileValid = await profileForm.trigger()
        const addressValid = await addressForm.trigger()
        if (!profileValid || !addressValid) return

        const profileValues = profileForm.getValues()
        const addressValues = addressForm.getValues()

        const countryId = Number.isInteger(+addressValues.country)
            ? +addressValues.country
            : null
        const stateId = Number.isInteger(+addressValues.state)
            ? +addressValues.state
            : null
        const cityId = Number.isInteger(+addressValues.city)
            ? +addressValues.city
            : null

        const formattedAddress = {
            country: countryIdMap.get(countryId) || "",
            countryId,
            state: stateIdMap.get(stateId) || "",
            stateId,
            city: cityIdMap.get(cityId) || "",
            cityId,
            postalCode: addressValues.postalCode || "",
            street: addressValues.street || "",
        }

        const allProfileFields = {
            ...profileValues,
            address: formattedAddress,
        }

        setLoading(true)
        const response = await showPromise(
            updateUser(allProfileFields),
            {
                loading: "Updating profile...",
                success: "Profile updated successfully!",
                error: "Failed to update profile!",
            },
            {throwOnError: true}
        )

        profileForm.reset(profileValues, {keepDirty: false})
        addressForm.reset(addressValues, {keepDirty: false})
        setShowSave(false)

        const updatedAuth = response.data
        localStorage.setItem("auth", JSON.stringify(updatedAuth))
        setAuth(updatedAuth)
    } catch (e) {
        console.error("Error updating user profile: ", e)
    } finally {
        setLoading(false)
    }
}

const handleResetAll = () => {
    profileForm.reset({
        firstName: user.firstName || "",
        lastName: user.lastName || "",
        email: user.email || "",
        phone: user.phone || "",
    })

    addressForm.reset({
        country: user?.address?.countryId || "",
        state: user?.address?.stateId || "",
        city: user?.address?.cityId || "",
        postalCode: user.address?.postalCode || "",
        street: user.address?.street || "",
    })

    setShowSave(false)
}

const handleImageUpload = async (e) => {
    const file = e.target.files[0]
    if (!file) return

    const formData = new FormData()
    formData.append("avatar", file, `${user.id}.jpg`)

    setLoading(true)
    try {
        await showPromise(
            uploadAvatar(formData),
            {
                loading: "Uploading avatar...",
                success: "Avatar uploaded!",
                error: "Failed to upload avatar!",
            },
            {throwOnError: true}
        )

        const imagePath = `/data/User Avatars/${user.id}.jpg?${Date.now()}`
        setProfileImage(imagePath)

        const updatedAuth = {
            ...auth,
            user: {
                ...auth.user,
                profileAvatar: imagePath,
            },
        }
        localStorage.setItem("auth", JSON.stringify(updatedAuth))
        setAuth(updatedAuth)
    } catch (err) {
        console.error("Avatar upload error:", err)
    } finally {
        setLoading(false)
    }
}

const confirmDeactivateAccount = async () => {
    try {
        setLoading(true)
        await showPromise(
            deactivateUserAccount(),
            {
                loading: "Deactivating account...",
                success: "Account deactivated successfully!",
                error: "Failed to deactivate account!",
            }
        )
        navigate("/")
        setTimeout(logout, 300)
    } catch (e) {
        console.error(e)
    } finally {
        setShowDeactivateModal(false)
        setLoading(false)
    }
}

const getInitials = (firstName, lastName) =>
    `${firstName?.charAt(0) || ""}${lastName?.charAt(0) || ""}`.toUpperCase()

return (
    <div className="relative space-y-10 pb-5">
        <div className="flex items-center space-x-6 p-6 bg-gradient-to-r from-gray-50 to-gray-100 rounded-xl">
            <div className="relative">
                <UserAvatar
                    profileAvatar={profileImage || user.profileAvatar}
                    name={getInitials(user.firstName, user.lastName)}
                    size="xl"
                    className="border-4 border-white shadow-lg"
                />
                <label
                    className="absolute bottom-0 right-0 bg-[#1C387F] text-white p-2 rounded-full cursor-pointer hover:bg-[#153066] transition-all duration-200 shadow-lg">
                    <Camera size={14}/>
                    <input
                        type="file"
                        accept="image/*"
                        onChange={handleImageUpload}
                        className="hidden"
                    />
                </label>
            </div>
            <div>
                <h3 className="text-xl font-semibold" style={{color: "#1C387F"}}>
                    {user.fullName}
                </h3>
                <p className="text-gray-600 mb-2">{user.email}</p>
                <p className="text-sm text-gray-500">
                    Upload a new profile picture to personalize your account
                </p>
            </div>
        </div>

        <div ref={formSectionRef}>
            <div className="bg-white border border-gray-100 rounded-xl p-6 shadow-sm mb-10">
                <h3 className="text-xl font-semibold mb-6 flex items-center" style={{color: "#1C387F"}}>
                    <User className="mr-2" size={20}/> Basic Information
                </h3>
                <form className="space-y-6">
                    <div className="grid gap-6 md:grid-cols-2">
                        <FormInput
                            label="First Name"
                            required
                            placeholder="Enter First Name"
                            {...profileForm.register("firstName")}
                            error={profileForm.formState.errors.firstName?.message}
                        />
                        <FormInput
                            label="Last Name"
                            required
                            placeholder="Enter Last Name"
                            {...profileForm.register("lastName")}
                            error={profileForm.formState.errors.lastName?.message}
                        />
                    </div>

                    <FormInput
                        label="Email"
                        required
                        placeholder="Enter Email Address"
                        {...profileForm.register("email")}
                        error={profileForm.formState.errors.email?.message}
                        autoComplete="email"
                    />

                    <div className="w-full">
                        <label className="block text-sm font-medium mb-1">
                            Phone Number <span className="text-red-600">*</span>
                        </label>
                        <Controller
                            name="phone"
                            control={profileForm.control}
                            render={({field}) => (
                                <PhoneInput
                                    {...field}
                                    country="vn"
                                    inputProps={{
                                        name: "phone",
                                        required: true,
                                        autoComplete: "tel",
                                    }}
                                    inputClass={`w-full text-sm px-3 py-2 border rounded-lg bg-white focus:outline-none 
                                  focus:ring-2 focus:ring-[#1C387F] focus:border-[#1C387F]
                                  ${
                                        profileForm.formState.errors.phone
                                            ? "border-red-500 focus:ring-red-500"
                                            : "border-gray-300"
                                    }`}
                                    inputStyle={{width: "100%", color: "#111827"}}
                                />
                            )}
                        />
                        {profileForm.formState.errors.phone && (
                            <p className="text-sm text-red-500 mt-1">
                                {profileForm.formState.errors.phone.message}
                            </p>
                        )}
                    </div>
                </form>
            </div>

            <div className="bg-white border border-gray-100 rounded-xl p-6 shadow-sm">
                <h3 className="text-xl font-semibold mb-6 flex items-center" style={{color: "#1C387F"}}>
                    <MapPin className="mr-2" size={20}/> Address Information
                </h3>
                <form className="space-y-6">
                    {addressForm.formState.errors[""]?.message && (
                        <p className="text-sm text-red-500 mb-2">
                            {addressForm.formState.errors[""]?.message}
                        </p>
                    )}

                    <div className="grid gap-6 md:grid-cols-2">
                        <div>
                            <label className="block text-sm font-medium text-gray-900 mb-1">
                                Country <span className="text-red-600">*</span>
                            </label>
                            <Controller
                                name="country"
                                control={addressForm.control}
                                render={({field}) => (
                                    <select
                                        {...field}
                                        id="country"
                                        disabled={isLoadingCountries || isErrorCountries}
                                        className={`w-full text-sm px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-[#1C387F] focus:border-[#1C387F] cursor-pointer transition-all ${
                                            addressForm.formState.errors.country
                                                ? "border-red-500 focus:ring-red-500"
                                                : "border-gray-300"
                                        } ${
                                            isLoadingCountries || isErrorCountries
                                                ? "bg-gray-100 cursor-not-allowed"
                                                : "bg-white"
                                        }`}
                                    >
                                        {isLoadingCountries ? (
                                            <option>Loading countries…</option>
                                        ) : isErrorCountries ? (
                                            <option>Error loading countries</option>
                                        ) : (
                                            <>
                                                <option value="">Select Country</option>
                                                {countries.map(({value, label}) => (
                                                    <option key={value} value={value}>
                                                        {label}
                                                    </option>
                                                ))}
                                            </>
                                        )}
                                    </select>
                                )}
                            />
                            {addressForm.formState.errors.country && (
                                <p className="text-sm text-red-500 mt-1">
                                    {addressForm.formState.errors.country.message}
                                </p>
                            )}
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-900 mb-1">
                                State / Province <span className="text-red-600">*</span>
                            </label>
                            <Controller
                                name="state"
                                control={addressForm.control}
                                render={({field}) => (
                                    <select
                                        {...field}
                                        id="state"
                                        disabled={!selectedCountry || isLoadingStates || isErrorStates}
                                        className={`w-full text-sm px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-[#1C387F] focus:border-[#1C387F] cursor-pointer transition-all ${
                                            addressForm.formState.errors.state
                                                ? "border-red-500 focus:ring-red-500"
                                                : "border-gray-300"
                                        } ${
                                            !selectedCountry || isLoadingStates || isErrorStates
                                                ? "bg-gray-100 cursor-not-allowed"
                                                : "bg-white"
                                        }`}
                                    >
                                        {isLoadingStates ? (
                                            <option>Loading states…</option>
                                        ) : isErrorStates ? (
                                            <option>Error loading states</option>
                                        ) : (
                                            <>
                                                <option value="">Select State</option>
                                                {statesList.map(({value, label}) => (
                                                    <option key={value} value={value}>
                                                        {label}
                                                    </option>
                                                ))}
                                            </>
                                        )}
                                    </select>
                                )}
                            />
                            {addressForm.formState.errors.state && (
                                <p className="text-sm text-red-500 mt-1">
                                    {addressForm.formState.errors.state.message}
                                </p>
                            )}
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-900 mb-1">
                                City / District <span className="text-red-600">*</span>
                            </label>
                            <Controller
                                name="city"
                                control={addressForm.control}
                                render={({field}) => (
                                    <select
                                        {...field}
                                        id="city"
                                        disabled={!selectedState || isLoadingCities || isErrorCities}
                                        className={`w-full text-sm px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-[#1C387F] focus:border-[#1C387F] cursor-pointer transition-all ${
                                            addressForm.formState.errors.city
                                                ? "border-red-500 focus:ring-red-500"
                                                : "border-gray-300"
                                        } ${
                                            !selectedState || isLoadingCities || isErrorCities
                                                ? "bg-gray-100 cursor-not-allowed"
                                                : "bg-white"
                                        }`}
                                    >
                                        {isLoadingCities ? (
                                            <option>Loading cities…</option>
                                        ) : isErrorCities ? (
                                            <option>Error loading cities</option>
                                        ) : (
                                            <>
                                                <option value="">Select City</option>
                                                {citiesList.map(({value, label}) => (
                                                    <option key={value} value={value}>
                                                        {label}
                                                    </option>
                                                ))}
                                            </>
                                        )}
                                    </select>
                                )}
                            />
                            {addressForm.formState.errors.city && (
                                <p className="text-sm text-red-500 mt-1">
                                    {addressForm.formState.errors.city.message}
                                </p>
                            )}
                        </div>

                        <FormInput
                            label="Postal / Zip Code"
                            required
                            placeholder="Enter Postal Code"
                            {...addressForm.register("postalCode")}
                            error={addressForm.formState.errors.postalCode?.message}
                        />
                    </div>

                    <FormInput
                        label="Street Address"
                        required
                        placeholder="Enter Street Address"
                        {...addressForm.register("street")}
                        error={addressForm.formState.errors.street?.message}
                        className="w-full"
                    />
                </form>
            </div>
        </div>

        <div className="bg-red-50 border border-red-200 rounded-xl p-6 shadow-sm">
            <div className="flex items-start space-x-4">
                <AlertTriangle className="text-red-600 mt-1 flex-shrink-0" size={24}/>
                <div className="flex-1">
                    <h4 className="text-lg font-semibold text-red-900 mb-2">Danger Zone</h4>
                    <p className="text-sm text-red-700 mb-4">
                        Temporarily deactivate your account. You can reactivate it anytime by contacting Bookify
                        Support.
                    </p>
                    <button
                        onClick={() => setShowDeactivateModal(true)}
                        disabled={loading}
                        className="bg-red-600 text-white text-sm font-semibold py-2 px-6 rounded-lg hover:bg-red-700 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed shadow-md hover:shadow-lg"
                    >
                        {loading ? "Processing..." : "Deactivate Account"}
                    </button>
                </div>
            </div>
        </div>

        {showSave && (
            <div className="fixed left-1/2 bottom-6 transform -translate-x-1/2 flex space-x-4">
                <button
                    onClick={handleSaveAll}
                    disabled={loading}
                    className="flex items-center space-x-2 bg-[#1C387F] text-white text-sm font-semibold py-3 px-8 rounded-full shadow-lg hover:shadow-xl hover:bg-[#153066] transform hover:-translate-y-0.5 transition-all duration-200 ease-in-out disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
                >
                    <Save size={16}/>
                    <span>{loading ? "Saving..." : "Save Changes"}</span>
                </button>

                <button
                    onClick={handleResetAll}
                    disabled={loading}
                    className="flex items-center space-x-2 bg-[#F06548] text-white text-sm font-semibold py-3 px-8 rounded-full shadow-lg hover:shadow-xl hover:bg-[#D64534] transform hover:-translate-y-0.5 transition-all duration-200 ease-in-out disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
                >
                    <span>Reset</span>
                </button>
            </div>
        )}

        <ConfirmationModal
            open={showDeactivateModal}
            onClose={() => setShowDeactivateModal(false)}
            onConfirm={confirmDeactivateAccount}
            loading={loading}
            title="Confirm Deactivation"
            description="This account is now inactive. Contact Bookify Support to reactivate."
            confirmText="Yes, Deactivate"
        />
    </div>
)
}
