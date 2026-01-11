import {useContext, useEffect, useState, useRef, useMemo} from "react"
import {useForm, Controller} from "react-hook-form"
import {yupResolver} from "@hookform/resolvers/yup"
import {checkoutSchema} from "@utils/validate.js"
import {MapPin} from "lucide-react"
import FormInput from "@u_components/shared/FormInput.jsx"
import PhoneInput from "react-phone-input-2"
import "react-phone-input-2/lib/style.css"
import OrderSummary from "@u_components/checkout/OrderSummary.jsx"
import {useNavigate} from "react-router-dom"
import CheckoutProgress from "@u_components/checkout/CheckoutProgress.jsx"
import {useAddressData} from "@u_hooks/useAddressData.js"
import {AuthContext} from "@contexts/AuthContext.jsx"
import {CheckoutContext} from "@contexts/CheckoutContext.jsx"

export default function CheckoutPage() {
    const {auth} = useContext(AuthContext)
    const {
        selectedItems,
        totals,
        shippingAddress,
        usingSaved,
        setShippingAddress,
        setUsingSaved,
        setOrderCompleted,
    } = useContext(CheckoutContext)

    const navigate = useNavigate()

    useEffect(() => {
        if (!selectedItems || selectedItems.length === 0) {
            navigate("/cart", {replace: true})
        }
    }, [selectedItems, navigate])

    const {itemsCount, subtotal, shipping, taxes, discount, total} = totals

    const {
        register,
        handleSubmit,
        control,
        watch,
        setValue,
        reset,
        formState: {errors, isDirty},
    } = useForm({
        resolver: yupResolver(checkoutSchema),
        defaultValues: shippingAddress || {},
    })

    const selectedCountry = watch("country")
    const selectedState = watch("state")

    const [userAddress, setUserAddress] = useState(null)
    const prevCountryRef = useRef(null)
    const prevStateRef = useRef(null)

    useEffect(() => {
        if (auth?.user?.address) {
            const addr = auth.user.address
            setUserAddress({
                firstName: auth.user.firstName || "",
                lastName: auth.user.lastName || "",
                email: auth.user.email || "",
                phoneNumber: auth.user.phone || "",
                street: addr.street,
                city: addr.cityId,
                state: addr.stateId,
                postalCode: addr.postalCode,
                country: addr.countryId,
            })
        }
    }, [auth])

    useEffect(() => {
        if (usingSaved && userAddress) populateSaved()
    }, [usingSaved, userAddress])

    useEffect(() => {
        if (prevCountryRef.current && selectedCountry !== +prevCountryRef.current) {
            setValue("state", "")
            setValue("city", "")
        }
        prevCountryRef.current = selectedCountry
    }, [selectedCountry, setValue])

    useEffect(() => {
        if (prevStateRef.current && selectedState !== +prevStateRef.current) {
            setValue("city", "")
        }
        prevStateRef.current = selectedState
    }, [selectedState, setValue])

    useEffect(() => {
        if (usingSaved && isDirty) setUsingSaved(false)
    }, [usingSaved, isDirty, setUsingSaved])

    const {
        countries,
        statesList,
        citiesList,
        isLoadingCountries,
        isErrorCountries,
        isLoadingStates,
        isErrorStates,
        isLoadingCities,
        isErrorCities,
    } = useAddressData(selectedCountry, selectedState)

    const countryIdMap = useMemo(() => new Map(countries.map(c => [c.value, c.label])), [countries])
    const stateIdMap = useMemo(() => new Map(statesList.map(s => [s.value, s.label])), [statesList])
    const cityIdMap = useMemo(() => new Map(citiesList.map(c => [c.value, c.label])), [citiesList])

    const populateSaved = () => {
        if (!userAddress) return
        setUsingSaved(true)

        prevCountryRef.current = userAddress.country
        prevStateRef.current = userAddress.state

        reset({...userAddress})
    }

    const onSubmit = (data) => {
        const formatted = {
            ...data,
            country: countryIdMap.get(+data.country),
            countryId: +data.country,
            state: stateIdMap.get(+data.state),
            stateId: +data.state,
            city: cityIdMap.get(+data.city),
            cityId: +data.city,
        }
        setShippingAddress(formatted)
        setOrderCompleted(false)
        navigate("/payment")
    }

    return (
        <div className="bg-neutral-100">
            <div className="max-w-screen-xl mx-auto px-4 py-16">
                <CheckoutProgress/>

                <h1 className="text-3xl font-bold text-[#D70018] mb-6">
                    Checkout
                </h1>

                <form onSubmit={handleSubmit(onSubmit)} className="grid lg:grid-cols-7 gap-8">
                    <div className="lg:col-span-5 bg-white rounded-xl shadow-lg p-10 space-y-6">

                        <div className="grid md:grid-cols-2 gap-6">
                            <FormInput label="First Name" required {...register("firstName")} error={errors.firstName?.message}/>
                            <FormInput label="Last Name" required {...register("lastName")} error={errors.lastName?.message}/>
                        </div>

                        <FormInput label="Email" required type="email" {...register("email")} error={errors.email?.message}/>

                        <div>
                            <label className="block text-sm font-medium mb-1">
                                Phone Number <span className="text-[#D70018]">*</span>
                            </label>
                            <Controller
                                name="phoneNumber"
                                control={control}
                                render={({field}) => (
                                    <PhoneInput
                                        {...field}
                                        country="vn"
                                        inputClass="!w-full !text-sm !px-3 !py-2 !border !rounded !border-gray-300 focus:!border-[#D70018]"
                                    />
                                )}
                            />
                            {errors.phoneNumber && <p className="text-sm text-red-600 mt-1">{errors.phoneNumber.message}</p>}
                        </div>

                        <div className="grid md:grid-cols-2 gap-6">
                            <SelectField label="Country" name="country" list={countries} control={control}
                                         isLoading={isLoadingCountries} isError={isErrorCountries} error={errors.country?.message}/>
                            <SelectField label="State" name="state" list={statesList} control={control}
                                         isLoading={isLoadingStates} isError={isErrorStates}
                                         error={errors.state?.message} disabled={!selectedCountry}/>
                        </div>

                        <div className="grid md:grid-cols-2 gap-6">
                            <SelectField label="City" name="city" list={citiesList} control={control}
                                         isLoading={isLoadingCities} isError={isErrorCities}
                                         error={errors.city?.message} disabled={!selectedState}/>
                            <FormInput label="Postal Code" required {...register("postalCode")} error={errors.postalCode?.message}/>
                        </div>

                        <FormInput label="Street Address" required {...register("street")} error={errors.street?.message}/>

                        {userAddress && !usingSaved && (
                            <div className="text-center pt-4">
                                <button
                                    type="button"
                                    onClick={populateSaved}
                                    className="inline-flex items-center gap-2 px-6 py-3 bg-black text-white rounded-lg hover:bg-[#D70018] transition"
                                >
                                    <MapPin size={16}/>
                                    Use saved address
                                </button>
                            </div>
                        )}
                    </div>

                    <aside className="lg:col-span-2 sticky top-8">
                        <OrderSummary
                            itemsCount={itemsCount}
                            subtotal={subtotal}
                            shipping={shipping}
                            taxes={taxes}
                            discount={discount}
                            total={total}
                            goToPayment={handleSubmit(onSubmit)}
                        />
                    </aside>
                </form>
            </div>
        </div>
    )
}

function SelectField({label, name, list, control, isLoading, isError, error, disabled}) {
    return (
        <div>
            <label className="block text-sm font-medium mb-1">
                {label} <span className="text-[#D70018]">*</span>
            </label>
            <Controller
                name={name}
                control={control}
                render={({field}) => (
                    <select
                        {...field}
                        disabled={disabled || isLoading || isError}
                        className={`w-full px-3 py-2 text-sm border rounded
                            ${error ? "border-red-600" : "border-gray-300"}
                            focus:border-[#D70018] focus:ring-[#D70018]
                            ${disabled ? "bg-gray-100 cursor-not-allowed" : "bg-white"}`}
                    >
                        <option value="">Select {label}</option>
                        {list.map(o => (
                            <option key={o.value} value={o.value}>{o.label}</option>
                        ))}
                    </select>
                )}
            />
            {error && <p className="text-sm text-red-600 mt-1">{error}</p>}
        </div>
    )
}
