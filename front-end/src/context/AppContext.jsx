
import React, {
    createContext,
    useContext,
    useEffect,
    useState,
} from "react";

import {
    getCurrentUser,
    logoutRequest,
} from "../services/authService";

const C = createContext(null);

export function AppProvider({ children }) {
    const [user, setUser] = useState(null);
    const [authLoading, setAuthLoading] = useState(true);
    const [toast, setToast] = useState("");

    useEffect(() => {
        let active = true;

        async function loadUser() {
            try {
                const data = await getCurrentUser();


                if (active && data.authenticated && data.user) {
                    setUser({
                        ...data.user,
                        role: String(
                            data.role || data.user.role || ""
                        ).trim().toLowerCase(),
                        balance: data.balance,
                        company: data.company ?? null,
                    });
                }

            } catch {
                if (active) setUser(null);
            } finally {
                if (active) setAuthLoading(false);
            }
        }

        loadUser();

        return () => {
            active = false;
        };
    }, []);

    function login(userData) {
        setUser(userData);
    }

    async function logout() {
        try {
            await logoutRequest();
        } finally {
            setUser(null);
        }
    }

    function notify(text) {
        setToast(text);
        setTimeout(() => setToast(""), 4000);
    }

    return (
        <C.Provider
            value={{
                user,
                login,
                logout,
                notify,
                authLoading,
            }}
        >
            {children}

            {toast && (
                <div className="toast" role="status">
                    ✓ {toast}
                </div>
            )}
        </C.Provider>
    );
}

export const useApp = () => useContext(C);

export function workdays(start, end) {
    let n = 0;

    for (
        let d = new Date(start + "T12:00:00");
        d <= new Date(end + "T12:00:00");
        d.setDate(d.getDate() + 1)
    ) {
        if (d.getDay() !== 0 && d.getDay() !== 6) {
            n++;
        }
    }

    return n;
}
