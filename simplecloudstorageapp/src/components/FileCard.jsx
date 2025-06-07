import React, { useMemo, useEffect } from "react"; // Added useEffect for logging
import {
  FiDownload,
  FiRefreshCw,
  FiLayers,
  FiTrash2,
  FiClock, // Although not used directly in FileCard's visible JSX, kept for consistency
  FiGlobe,
  FiLock,
  FiHardDrive,
  FiCalendar,
  FiTag,
  FiX, // Not used, consider removing if not needed
  FiArchive, // Not used, consider removing if not needed
} from "react-icons/fi";
import { Tooltip } from "react-tooltip";
import "react-tooltip/dist/react-tooltip.css";

function FileCard({
  file,
  handleDownload,
  handleDelete,
  handleViewVersions,
  setReplaceFileId,
  formatFileSize,
  // Props related to modal are removed as FileCard no longer renders the modal
}) {
  // Log when a FileCard component renders or re-renders
  useEffect(() => {
    console.log("FileCard rendered for file:", file.filename, " (ID:", file.fileId, ")");
  }, [file]); // Log when the 'file' prop changes

  const typeParts = file.filetype ? file.filetype.split("/") : ["unknown"];
  console.log(`FileCard: Processing file type for ${file.filename}:`, typeParts);


  // Format date for display within the card itself
  const formatDate = (dateString) => {
    if (!dateString) {
      console.warn(`FileCard: No created_at date for ${file.filename}`);
      return "Unknown date";
    }
    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) {
        console.warn(`FileCard: Invalid date string for ${file.filename}: ${dateString}`);
        return "Invalid date";
      }
      return date.toLocaleDateString("en-US", {
        year: "numeric",
        month: "short",
        day: "numeric",
      });
    } catch (e) {
      console.error(`FileCard: Error parsing date for ${file.filename}: ${dateString}`, e);
      return "Invalid date";
    }
  };

  return (
    <div className="file-card">
      {/* File Header */}
      <div className="file-header">
        <div className="file-type-badge">
          <span className={`file-type ${typeParts[0]}`}>
            {typeParts[1] || typeParts[0]}
          </span>
          <span className={`file-privacy ${file.public ? "public" : "private"}`}>
            {file.public ? (
              <>
                <FiGlobe className="privacy-icon" /> Public
              </>
            ) : (
              <>
                <FiLock className="privacy-icon" /> Private
              </>
            )}
          </span>
        </div>
      </div>

      {/* File Body */}
      <div className="file-body">
        <div className="file-info">
          <h3 className="file-name" title={file.filename}>
            {file.filename}
          </h3>
          <p className="file-meta">
            <FiHardDrive className="meta-icon" />
            <span>{formatFileSize(file.filesize)}</span>
            {file.created_at && (
              <>
                <FiCalendar className="meta-icon" />
                <span>{formatDate(file.created_at)}</span>
              </>
            )}
          </p>
        </div>

        {file.tags?.length > 0 && (
          <div className="file-tags">
            {console.log(`FileCard: Tags for ${file.filename}:`, file.tags)}
            {file.tags.map((tag) => (
              <span key={tag} className="file-tag">
                <FiTag className="tag-icon" />
                {tag}
              </span>
            ))}
          </div>
        )}
      </div>

      {/* File Actions with react-tooltip */}
      <div className="file-actions">
        <button
          data-tooltip-id="download-tooltip"
          data-tooltip-content="Download"
          onClick={() => {
            console.log("FileCard: Download button clicked for file ID:", file.fileId);
            handleDownload(file.fileId);
          }}
          className="action-button download"
        >
          <FiDownload />
          <span className="action-label">Download</span>
        </button>

        <button
          data-tooltip-id="replace-tooltip"
          data-tooltip-content="Replace"
          onClick={() => {
            console.log("FileCard: Replace button clicked for file ID:", file.fileId);
            setReplaceFileId(file.fileId);
          }}
          className="action-button replace"
        >
          <FiRefreshCw />
          <span className="action-label">Replace</span>
        </button>

        <button
          data-tooltip-id="versions-tooltip"
          data-tooltip-content="View Versions"
          onClick={() => {
            console.log("FileCard: View Versions button clicked for file ID:", file.fileId);
            handleViewVersions(file.fileId);
          }}
          className="action-button versions"
        >
          <FiLayers />
          <span className="action-label">Versions</span>
        </button>

        <button
          data-tooltip-id="delete-tooltip"
          data-tooltip-content="Delete"
          onClick={() => {
            console.log("FileCard: Delete button clicked for file ID:", file.fileId);
            handleDelete(file.fileId);
          }}
          className="action-button delete"
        >
          <FiTrash2 />
          <span className="action-label">Delete</span>
        </button>

        {/* Tooltip elements */}
        <Tooltip id="download-tooltip" />
        <Tooltip id="replace-tooltip" />
        <Tooltip id="versions-tooltip" />
        <Tooltip id="delete-tooltip" />
      </div>
    </div>
  );
}

export default FileCard;