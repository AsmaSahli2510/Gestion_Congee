
import React, { useState } from "react";
import { Eye, EyeOff, Mail, Lock } from "lucide-react";

export default function AuthFields({
  password = true,
  newPassword = false,
  email = "",
  setEmail = () => {},
  passwordValue = "",
  setPasswordValue = () => {},
}) {
  const [show, setShow] = useState(false);

  return (
    <>
      <label className="field" htmlFor="email">
        Adresse email professionnelle
      </label>

      <div className="input-box">
        <Mail aria-hidden="true" />

        <input
          type="email"
          name="email"
          id="email"
          autoComplete="username"
          placeholder="vous@entreprise.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
      </div>

      {password && (
        <>
          <label className="field" htmlFor="password">
            Mot de passe
          </label>

          <div className="input-box">
            <Lock aria-hidden="true" />

            <input
              type={show ? "text" : "password"}
              name="password"
              id="password"
              autoComplete={
                newPassword ? "new-password" : "current-password"
              }
              minLength={newPassword ? 8 : 1}
              placeholder={
                newPassword
                  ? "8 caractères minimum"
                  : "Saisissez votre mot de passe"
              }
              value={passwordValue}
              onChange={(e) => setPasswordValue(e.target.value)}
              required
            />

            <button
              type="button"
              className="eye"
              aria-label={
                show
                  ? "Masquer le mot de passe"
                  : "Afficher le mot de passe"
              }
              onClick={() => setShow(!show)}
            >
              {show ? <EyeOff /> : <Eye />}
            </button>
          </div>
        </>
      )}
    </>
  );
}
