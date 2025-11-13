import { env } from '@/config/env';

interface GoogleLoginButtonProps {
  text?: 'signin' | 'signup' | 'continue';
}

/**
 * Google Login Button Component
 * Follows official Google Sign-In Branding Guidelines
 * https://developers.google.com/identity/branding-guidelines
 *
 * Specifications:
 * - Light theme: White background (#FFFFFF), Gray border (#747775)
 * - Text color: #1F1F1F
 * - Font: Roboto Medium, 14px, 20px line-height
 * - Padding: 12px left/right, 10px gap between logo and text
 *
 * Redirects user to backend OAuth2 endpoint for Google login
 */
export function GoogleLoginButton({ text = 'signin' }: GoogleLoginButtonProps) {
  const buttonText = {
    signin: 'Sign in with Google',
    signup: 'Sign up with Google',
    continue: 'Continue with Google',
  };

  function handleLogin() {
    window.location.href = env.oauth2LoginUrl;
  }

  return (
    <button
      onClick={handleLogin}
      type="button"
      aria-label={buttonText[text]}
      className="flex min-w-[200px] cursor-pointer items-center justify-center rounded border border-[#747775] bg-transparent px-3 py-2.5 text-sm font-medium leading-5 text-white transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 active:bg-gray-100"
    >
      {/* Google "G" Logo - Official SVG with standard colors */}
      <svg
        width="18"
        height="18"
        viewBox="0 0 18 18"
        xmlns="http://www.w3.org/2000/svg"
        className="mr-2.5"
        aria-hidden="true"
      >
        <g fill="none" fillRule="evenodd">
          <path
            d="M17.64 9.205c0-.639-.057-1.252-.164-1.841H9v3.481h4.844a4.14 4.14 0 01-1.796 2.716v2.259h2.908c1.702-1.567 2.684-3.875 2.684-6.615z"
            fill="#4285F4"
          />
          <path
            d="M9 18c2.43 0 4.467-.806 5.956-2.18l-2.908-2.259c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 009 18z"
            fill="#34A853"
          />
          <path
            d="M3.964 10.71A5.41 5.41 0 013.682 9c0-.593.102-1.17.282-1.71V4.958H.957A8.996 8.996 0 000 9c0 1.452.348 2.827.957 4.042l3.007-2.332z"
            fill="#FBBC05"
          />
          <path
            d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 00.957 4.958L3.964 7.29C4.672 5.163 6.656 3.58 9 3.58z"
            fill="#EA4335"
          />
        </g>
      </svg>

      {/* Button Text */}
      <span className="font-medium">{buttonText[text]}</span>
    </button>
  );
}
