import styled from 'styled-components';
import { useTheme } from '../context/ThemeContext';

const ThemeToggle = () => {
    const { theme, toggleTheme } = useTheme();

    return (
        <StyledWrapper>
            <label className="switch">
                <input
                    id="theme-checkbox"
                    type="checkbox"
                    checked={theme === 'dark'}
                    onChange={toggleTheme}
                />
                <span className="slider">
                    <div className="star star_1" />
                    <div className="star star_2" />
                    <div className="star star_3" />
                    <svg viewBox="0 0 16 16" className="cloud_1 cloud">
                        <path transform="matrix(.77976 0 0 .78395-299.99-418.63)" fill="#fff" d="m391.84 540.91c-.421-.329-.949-.524-1.523-.524-1.351 0-2.451 1.084-2.485 2.435-1.395.526-2.388 1.88-2.388 3.466 0 1.874 1.385 3.423 3.182 3.667v.034h12.73v-.006c1.775-.104 3.182-1.584 3.182-3.395 0-1.747-1.309-3.186-2.994-3.379.007-.106.011-.214.011-.322 0-2.707-2.271-4.901-5.072-4.901-2.073 0-3.856 1.202-4.643 2.925" />
                    </svg>
                </span>
            </label>
        </StyledWrapper>
    );
}

const StyledWrapper = styled.div`
  /* Theme Switch */
  /* The switch - the box around the slider */
  .switch {
    font-size: 17px;
    position: relative;
    display: inline-block;
    width: 3.5em; /* Slightly reduced width to fit navbar better */
    height: 2em; /* Slightly reduced height */
    border-radius: 30px;
    /* Box shadow removed to blend better with navbar */
  }

  /* Hide default HTML checkbox */
  .switch input {
    opacity: 0;
    width: 0;
    height: 0;
  }

  /* The slider */
  .slider {
    position: absolute;
    cursor: pointer;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: #fce570; /* Sun/Day color default */
    transition: 0.4s;
    border-radius: 30px;
    overflow: hidden;
    box-shadow: inset 0 0 5px rgba(0,0,0,0.2);
  }

  .slider:before {
    position: absolute;
    content: "";
    height: 1.4em;
    width: 1.4em;
    border-radius: 50%;
    left: 0.3em;
    bottom: 0.3em;
    background-color: #fff;
    transition: 0.4s;
    transition-timing-function: cubic-bezier(0.68, -0.55, 0.265, 1.55);
    box-shadow: 0 2px 5px rgba(0,0,0,0.2);
    z-index: 2;
  }

  /* Dark mode state (checked) */
  .switch input:checked + .slider {
    background-color: #2a2a2a; /* Night color */
  }

  .switch input:checked + .slider:before {
    transform: translateX(1.5em);
    background-color: #fff;
    box-shadow: inset -4px -2px 0 0 #e0e0e0; /* Moon crater effect */
  }

  /* Stars */
  .star {
    background-color: #fff;
    border-radius: 50%;
    position: absolute;
    width: 4px;
    height: 4px;
    transition: all 0.4s;
    opacity: 0;
    z-index: 1;
  }

  .star_1 {
    left: 0.8em;
    top: 0.6em;
  }

  .star_2 {
    left: 1.4em;
    top: 1.2em;
    width: 3px;
    height: 3px;
  }

  .star_3 {
    left: 0.5em;
    top: 1.3em;
    width: 2px;
    height: 2px;
  }

  /* Show stars in dark mode */
  .switch input:checked ~ .slider .star {
    opacity: 1;
    transform: scale(1);
    transition-delay: 0.1s;
  }

  /* Clouds for light mode */
  .cloud {
    width: 3em;
    position: absolute;
    bottom: -1em;
    left: 0.5em;
    opacity: 1; /* Visible by default (light mode) */
    transition: all 0.4s;
    z-index: 1;
    fill: #fff;
  }

  .switch input:checked ~ .slider .cloud {
    opacity: 0;
    transform: translateY(10px);
  }
`;

export default ThemeToggle;
