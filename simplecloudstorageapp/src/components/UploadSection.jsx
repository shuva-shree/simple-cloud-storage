import React from "react";
import { FiUpload } from "react-icons/fi";

function UploadSection({ selectedFile, setSelectedFile, isPublic, setIsPublic, handleUpload }) {
  return (
    <div className="upload-card">
      <div className="upload-section">
        <label className="file-upload-button">
          <FiUpload className="upload-icon" />
          Choose File
          <input
            type="file"
            onChange={(e) => setSelectedFile(e.target.files[0])}
            style={{ display: 'none' }}
          />
        </label>
        {selectedFile && (
          <div className="file-selection">
            <span>{selectedFile.name}</span>
            <div className="privacy-toggle">
              <label>
                <input
                  type="checkbox"
                  checked={isPublic}
                  onChange={(e) => setIsPublic(e.target.checked)}
                />
                <span className="toggle-switch"></span>
                {isPublic ? 'Public' : 'Private'}
              </label>
            </div>
          </div>
        )}
        <button 
          onClick={handleUpload} 
          className="upload-confirm-button"
          disabled={!selectedFile}
        >
          Upload
        </button>
      </div>
    </div>
  );
}

export default UploadSection;