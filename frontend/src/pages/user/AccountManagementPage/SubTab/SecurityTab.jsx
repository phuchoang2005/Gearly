import { useState } from "react"
import { useForm } from "react-hook-form"
import { yupResolver } from "@hookform/resolvers/yup"
import { changePasswordSchema } from "@utils/validate.js"
import FormInput from "@u_components/shared/FormInput.jsx"
import { showPromise } from "@utils/toast.js"
import { Lock } from "lucide-react"
import {changePassword} from "@u_services/userService.js";

export default function SecurityTab() {
    const [loading, setLoading] = useState(false)
    const passwordForm = useForm({
        resolver: yupResolver(changePasswordSchema),
    })

    const handlePasswordSubmit = async (data) => {
        setLoading(true)
        const formattedData = {
            oldPassword: data.currentPassword,
            newPassword: data.newPassword
        }
        await showPromise(
            changePassword(formattedData), {
            loading: "Updating password...",
            success: "Password updated successfully!",
            error: "Failed to update password!",
        })
        setLoading(false)
        passwordForm.reset()
    }

    return (
        <div className="bg-white border border-gray-100 rounded-xl p-6 shadow-sm">
            <h3 className="text-xl font-bold text-gray-900 mb-6 flex items-center">
                <Lock className="mr-2 text-[#1C387F]" size={20} />
                Change Password
            </h3>
            <form onSubmit={passwordForm.handleSubmit(handlePasswordSubmit)} className="space-y-6">
                <FormInput
                    label="Current Password"
                    type="password"
                    required
                    placeholder="Enter current password"
                    {...passwordForm.register("currentPassword")}
                    error={passwordForm.formState.errors.currentPassword?.message}
                />

                <FormInput
                    label="New Password"
                    type="password"
                    required
                    placeholder="Enter new password"
                    {...passwordForm.register("newPassword")}
                    error={passwordForm.formState.errors.newPassword?.message}
                />

                <FormInput
                    label="Confirm New Password"
                    type="password"
                    required
                    placeholder="Confirm new password"
                    {...passwordForm.register("confirmPassword")}
                    error={passwordForm.formState.errors.confirmPassword?.message}
                />

                <button
                    type="submit"
                    disabled={loading}
                    className="flex items-center space-x-2 bg-[#1C387F] text-white text-sm font-semibold py-3 px-8 rounded-lg
            shadow-lg hover:shadow-xl hover:bg-[#153066] transform hover:-translate-y-0.5
            transition-all duration-200 ease-in-out
            disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
                >
                    <Lock size={16} />
                    <span>{loading ? "Updating..." : "Update Password"}</span>
                </button>
            </form>
        </div>
    )
}
