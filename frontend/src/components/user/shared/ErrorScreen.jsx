export default function ErrorScreen() {
    return (
        <div className="flex min-h-screen items-center justify-center bg-red-50 px-4">
            {/* Card */}
            <div className="w-full max-w-md rounded-lg border border-red-200 bg-white p-6 shadow-lg">
                <h2 className="mb-2 text-xl font-semibold text-red-600">
                    Oops! Something went wrong
                </h2>
                <p className="text-gray-700">
                    We couldn’t load your data. Please refresh or try again later.
                </p>

                {/* Retry action */}
                <button
                    onClick={() => window.location.reload()}
                    className="mt-4 w-full rounded-md bg-red-600 py-2 text-white transition hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-400 focus:ring-offset-2"
                >
                    Retry
                </button>
            </div>
        </div>
    )
}