import React from 'react'
import {Link} from 'react-router-dom'

export default function HeaderBreadcrumb({title, crumbs}) {
    return (
        <div className="bg-gray-300/50 py-6 mb-6">
            <div className="max-w-screen-xl mx-auto px-4">
                <h1 className="text-3xl font-semibold mb-2 text-center">
                    {title}
                </h1>
                <div className="text-sm text-gray-600 text-center">
                    {crumbs.map((c, i) => (
                        <React.Fragment key={c.name}>
                            <Link to={c.path} className="hover:underline mx-1.5">
                                {c.name}
                            </Link>
                            {i < crumbs.length - 1 && ' / '}
                        </React.Fragment>
                    ))}
                </div>
            </div>
        </div>
    )
}
