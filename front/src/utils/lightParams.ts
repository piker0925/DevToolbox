export function buildFallbackParams(rawInput: string): Record<string, string> {
    return {input: rawInput, text: rawInput}
}
