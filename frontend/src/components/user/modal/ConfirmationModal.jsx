import {Dialog, Transition} from "@headlessui/react"
import {Fragment} from "react"

export default function ConfirmationModal({
                                              open,
                                              onClose,
                                              onConfirm,
                                              title = "Are you sure?",
                                              description = "",
                                              confirmText = "Confirm",
                                              cancelText = "Cancel",
                                              loading = false,
                                          }) {
    return (
        <Transition appear show={open} as={Fragment}>
            <Dialog as="div" className="relative z-50" onClose={onClose}>
                <Transition.Child
                    as={Fragment}
                    enter="ease-out duration-300"
                    enterFrom="opacity-0"
                    enterTo="opacity-100"
                    leave="ease-in duration-200"
                    leaveFrom="opacity-100"
                    leaveTo="opacity-0"
                >
                    <div className="fixed inset-0 bg-black/30"/>
                </Transition.Child>

                <div className="fixed inset-0 overflow-y-auto">
                    <div className="flex min-h-full items-center justify-center p-4 text-center">
                        <Transition.Child
                            as={Fragment}
                            enter="ease-out duration-300"
                            enterFrom="opacity-0 scale-95"
                            enterTo="opacity-100 scale-100"
                            leave="ease-in duration-200"
                            leaveFrom="opacity-100 scale-100"
                            leaveTo="opacity-0 scale-95"
                        >
                            <Dialog.Panel
                                className="w-full max-w-md transform overflow-hidden rounded-xl bg-white p-6 text-left align-middle shadow-xl transition-all">
                                <Dialog.Title className="text-lg font-semibold text-gray-900 mb-2">
                                    {title}
                                </Dialog.Title>
                                {description && (
                                    <Dialog.Description className="text-sm text-gray-600 mb-6">
                                        {description}
                                    </Dialog.Description>
                                )}
                                <div className="flex justify-end space-x-3">
                                    <button
                                        onClick={onClose}
                                        className="px-4 py-2 rounded-md text-sm border border-gray-300 hover:bg-gray-100"
                                    >
                                        {cancelText}
                                    </button>
                                    <button
                                        onClick={onConfirm}
                                        disabled={loading}
                                        className="px-4 py-2 rounded-md text-sm bg-red-600 text-white hover:bg-red-700 disabled:opacity-50"
                                    >
                                        {loading ? "Processing..." : confirmText}
                                    </button>
                                </div>
                            </Dialog.Panel>
                        </Transition.Child>
                    </div>
                </div>
            </Dialog>
        </Transition>
    )
}
