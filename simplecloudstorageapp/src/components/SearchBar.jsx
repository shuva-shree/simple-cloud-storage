import React from "react";
import { FiSearch } from "react-icons/fi";

function SearchBar({ searchQuery, setSearchQuery, selectedTag, setSelectedTag, tags, handleSearch }) {
  return (
    <form onSubmit={handleSearch} className="search-form">
      <div className="search-input-group">
        <FiSearch className="search-icon" />
        <input
          type="text"
          placeholder="Search files..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />
        <select 
          value={selectedTag}
          onChange={(e) => setSelectedTag(e.target.value)}
          className="tag-selector"
        >
          <option value="">All Tags</option>
          
        </select>
        <button type="submit" className="search-button">Search</button>
      </div>
    </form>
  );
}

export default SearchBar;