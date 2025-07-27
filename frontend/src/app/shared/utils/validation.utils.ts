/**
 * Validation utility functions
 */
export class ValidationUtils {
  /**
   * Validate UUID format
   */
  static isValidUUID(uuid: string): boolean {
    const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
    return uuidRegex.test(uuid);
  }

  /**
   * Validate URL format
   */
  static isValidURL(url: string): boolean {
    try {
      new URL(url);
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Validate email format
   */
  static isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }

  /**
   * Validate file size
   */
  static isValidFileSize(size: number, maxSize: number, minSize: number = 1): boolean {
    return size >= minSize && size <= maxSize;
  }

  /**
   * Validate chunk size
   */
  static isValidChunkSize(chunkSize: number, minSize: number, maxSize: number): boolean {
    return chunkSize >= minSize && chunkSize <= maxSize;
  }

  /**
   * Validate number range
   */
  static isInRange(value: number, min: number, max: number): boolean {
    return value >= min && value <= max;
  }

  /**
   * Validate string length
   */
  static isValidStringLength(str: string, minLength: number, maxLength: number): boolean {
    return str.length >= minLength && str.length <= maxLength;
  }

  /**
   * Validate that string is not empty or whitespace only
   */
  static isNonEmptyString(str: string): boolean {
    return typeof str === 'string' && str.trim().length > 0;
  }

  /**
   * Validate array is not empty
   */
  static isNonEmptyArray<T>(arr: T[]): boolean {
    return Array.isArray(arr) && arr.length > 0;
  }

  /**
   * Validate object has required properties
   */
  static hasRequiredProperties(obj: any, requiredProps: string[]): boolean {
    if (!obj || typeof obj !== 'object') return false;
    
    return requiredProps.every(prop => prop in obj);
  }

  /**
   * Validate MIME type format
   */
  static isValidMimeType(mimeType: string): boolean {
    const mimeTypeRegex = /^[a-zA-Z][a-zA-Z0-9][a-zA-Z0-9!#$&\-\^_]*\/[a-zA-Z0-9][a-zA-Z0-9!#$&\-\^_.]*$/;
    return mimeTypeRegex.test(mimeType);
  }

  /**
   * Validate HTTP status code
   */
  static isValidHttpStatus(status: number): boolean {
    return Number.isInteger(status) && status >= 100 && status < 600;
  }

  /**
   * Validate that value is a positive integer
   */
  static isPositiveInteger(value: number): boolean {
    return Number.isInteger(value) && value > 0;
  }

  /**
   * Validate that value is a non-negative integer
   */
  static isNonNegativeInteger(value: number): boolean {
    return Number.isInteger(value) && value >= 0;
  }

  /**
   * Validate timestamp
   */
  static isValidTimestamp(timestamp: number): boolean {
    return Number.isInteger(timestamp) && timestamp > 0 && timestamp <= Date.now();
  }

  /**
   * Validate percentage (0-100)
   */
  static isValidPercentage(value: number): boolean {
    return typeof value === 'number' && value >= 0 && value <= 100;
  }

  /**
   * Validate port number
   */
  static isValidPort(port: number): boolean {
    return Number.isInteger(port) && port >= 1 && port <= 65535;
  }

  /**
   * Validate IP address (IPv4)
   */
  static isValidIPv4(ip: string): boolean {
    const ipv4Regex = /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
    return ipv4Regex.test(ip);
  }

  /**
   * Validate hexadecimal string
   */
  static isValidHex(hex: string): boolean {
    const hexRegex = /^[0-9a-fA-F]+$/;
    return hexRegex.test(hex);
  }

  /**
   * Validate base64 string
   */
  static isValidBase64(base64: string): boolean {
    try {
      return btoa(atob(base64)) === base64;
    } catch {
      return false;
    }
  }

  /**
   * Sanitize string for safe display
   */
  static sanitizeString(str: string): string {
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#x27;')
      .replace(/\//g, '&#x2F;');
  }

  /**
   * Validate and sanitize filename
   */
  static sanitizeFilename(filename: string): string {
    return filename
      .replace(/[<>:"|?*\x00-\x1f]/g, '_')
      .replace(/\.\./g, '_')
      .replace(/^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])$/i, '_$1')
      .substring(0, 255);
  }

  /**
   * Validate JSON string
   */
  static isValidJSON(jsonString: string): boolean {
    try {
      JSON.parse(jsonString);
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Validate date string
   */
  static isValidDate(dateString: string): boolean {
    const date = new Date(dateString);
    return !isNaN(date.getTime());
  }

  /**
   * Validate that value is within allowed enum values
   */
  static isValidEnumValue<T>(value: T, enumObject: Record<string, T>): boolean {
    return Object.values(enumObject).includes(value);
  }

  /**
   * Validate configuration object structure
   */
  static validateConfig(config: any, schema: any): { valid: boolean; errors: string[] } {
    const errors: string[] = [];
    
    const validateProperty = (obj: any, schemaObj: any, path: string = '') => {
      for (const key in schemaObj) {
        const fullPath = path ? `${path}.${key}` : key;
        const schemaValue = schemaObj[key];
        const objValue = obj?.[key];
        
        if (schemaValue.required && (objValue === undefined || objValue === null)) {
          errors.push(`Missing required property: ${fullPath}`);
          continue;
        }
        
        if (objValue !== undefined && schemaValue.type) {
          const actualType = typeof objValue;
          if (actualType !== schemaValue.type) {
            errors.push(`Invalid type for ${fullPath}: expected ${schemaValue.type}, got ${actualType}`);
          }
        }
        
        if (objValue !== undefined && schemaValue.validate) {
          const isValid = schemaValue.validate(objValue);
          if (!isValid) {
            errors.push(`Validation failed for ${fullPath}`);
          }
        }
        
        if (objValue !== undefined && schemaValue.properties) {
          validateProperty(objValue, schemaValue.properties, fullPath);
        }
      }
    };
    
    validateProperty(config, schema);
    
    return {
      valid: errors.length === 0,
      errors
    };
  }
}
