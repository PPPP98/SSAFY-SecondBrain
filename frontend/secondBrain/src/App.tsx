import GoogleLoginButton from '@/components/GoogleLoginButton';

function App() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50">
      <div className="space-y-8 text-center">
        <div className="space-y-4">
          <h1 className="text-4xl font-bold text-gray-900">TailwindCSS Verification</h1>
          <p className="text-lg text-gray-600">Testing Google Login Button with TailwindCSS</p>
        </div>

        <div className="flex flex-col items-center space-y-4">
          <GoogleLoginButton text="signin" onClick={() => console.log('Sign in clicked')} />
          <GoogleLoginButton text="signup" onClick={() => console.log('Sign up clicked')} />
          <GoogleLoginButton text="continue" onClick={() => console.log('Continue clicked')} />
        </div>
      </div>
    </div>
  );
}

export default App;
