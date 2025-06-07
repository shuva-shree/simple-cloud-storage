import React from 'react';
import { useNavigate } from 'react-router-dom';
import '../styles/NavBar.css'; // We'll create this CSS file next

function NavBar() {
    const navigate = useNavigate();

    const handleLogout = () => {
        console.log("Logging out...");
        localStorage.removeItem('token'); // Clear the authentication token
        navigate('/login'); // Redirect to the login page
        console.log("Logged out and redirected to /login");
    };

    return (
        <nav className="navbar">
            <div className="navbar-logo">
                {/* You can replace this with an <img> tag if you have a logo image */}
                <span>CloudStorage</span>
            </div>
            <div className="navbar-actions">
                <button onClick={handleLogout} className="logout-button">
                    Logout
                </button>
            </div>
        </nav>
    );
}

export default NavBar;